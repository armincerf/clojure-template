(ns PROJECTNAMESPACE.PROJECTNAME.api.auth.domain
  (:require [buddy.auth.accessrules :as buddy.accessrules]
            [clojure.spec.alpha :as s]
            [clojure.tools.logging :as log]
            [ring.util.http-response :refer [see-other]]
            [PROJECTNAMESPACE.PROJECTNAME.api.customers.model :as customers.model]
            [PROJECTNAMESPACE.PROJECTNAME.api.admins.model :as admins.model]
            [PROJECTNAMESPACE.PROJECTNAME.api.auth.authorization-rules :as rules]
            [PROJECTNAMESPACE.PROJECTNAME.api.errors :as errors]
            [PROJECTNAMESPACE.PROJECTNAME.api.spec :as spec]
            [PROJECTNAMESPACE.PROJECTNAME.api.session :as session])
  (:import java.net.URLEncoder))

;; -- Authentication --

(defn log-out []
  (-> (see-other "/login")
      ;; delete the session and expire the PROJECTNAME_sid cookie
      (session/expire!)))

(s/fdef add-request-identity
  :args (s/cat :request map?
               :db ::spec/db
               :context :authn/context
               :account-id :session/login-id
               :account-type :session/account-type))

(defn add-request-identity
  "Add the requesting user identity to the request properties under key `:identity`
  if the account exists, otherwise adds `nil`."
  ([request db context account-id account-type]
   (add-request-identity request db context account-id account-type nil))
  ([request db context account-id account-type oauth-token]
   (let [find-by-id (case account-type
                      :admin #(admins.model/find-by-id db %)
                      :customer #(customers.model/find-by-id db %))]
     (prn "foo" find-by-id)
     (if-let [account (find-by-id account-id)]
       (let [employee-id (get-in request [:session :employee])
             {:keys [admin-id scope client-id]} oauth-token
             identity-map (-> account
                              (assoc :authentication-context context
                                     :account-type account-type))]
         (assoc request :identity identity-map))
       (do (log/error "account referenced by request does not exist"
                      {:account account-id
                       :context context
                       :account-type account-type})
           (assoc request :identity nil))))))

(defn- authenticate-via-session-cookie
  "Set the request identity (if not already set) from session cookie if provided."
  [request db]
  (let [{:keys [login account-type]} (:session request)]
    (if (and (nil? (:identity request)) login)
      (add-request-identity request db :session login account-type)
      request)))

(defn- authenticate
  "Authenticate the user via cookies that may be set. Adds the authentication
  context to the `:identity` map."
  [db req]
  (authenticate-via-session-cookie req db))

(defn wrap-authentication
  [handler db]
  (fn -wrap-authentication
    ([request]
     (handler (authenticate db request)))
    ([request respond raise]
     (def request request)
     (def respond respond)
     (def raise raise)
     (def handler handler)
     (def db db)
     (handler (authenticate db request) respond raise))))

;; -- Authorization --

(defn- account-not-found?
  "Was the account specified in the cookie not found? It may have been deleted."
  [req]
  (and (contains? req :identity) (nil? (:identity req))))

(defn on-authz-error
  "Global on-error handler for buddy accessrules."
  [{:keys [identity uri] :as req} handler-value]
  (let [error-code (case (:type handler-value)
                     ::rules/role :authz/forbidden
                     :authz/not-found)
        browser-request? (= "text/html" (-> req :muuntaja/response :raw-format))
        role-error? (= ::rules/role (:type handler-value))]
    (log/info :unauthorized-request
              {:failed-rule (:name handler-value)
               :uri uri
               :error-code error-code
               :identity identity})
    (cond
      ;; Log out (expire cookies) if account does not exist
      (and role-error? (account-not-found? req))
      (log-out)

      ;; Redirect to login if unauthorized
      (and role-error? browser-request?)
      (see-other (str "/login?next=" (URLEncoder/encode uri "UTF-8")))

      :else
      (throw (errors/exception error-code {:uri uri})))))

(defn compile-authorization
  "Authorize requests via access rules defined under `:authorize` in route maps.
  The value of `:authorize` can be one of:
     * buddy access rule handler function
     * buddy access rule handler composition, e.g. {:and [rule1 rule2]}
     * buddy access rule, e.g. {:handler rule3, :on-error foo}"
  [route-data _opts]
  (if-let [rule (:authorize route-data)]
    (let [options (->> (if (s/valid? :accessrules/rule rule)
                         (cond-> {:rules [rule]}
                           (not (:on-error rule)) (assoc :on-error on-authz-error))
                         {:rules [{:handler rule}] :on-error on-authz-error})
                       (merge {:policy :reject}))]
      (fn [handler] (buddy.accessrules/wrap-access-rules handler options)))
    {}))
