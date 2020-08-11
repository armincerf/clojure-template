(ns PROJECTNAMESPACE.PROJECTNAME.api.auth
  (:require [aleph.http :as http]
            [buddy.core.keys :as k]
            [buddy.sign.jwt :as jwt]
            [byte-streams :as bs]
            [cheshire.core :as json]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [PROJECTNAMESPACE.PROJECTNAME.api.customers.model :as customers.model]
            [PROJECTNAMESPACE.PROJECTNAME.api.admins.model :as admins.model]
            [PROJECTNAMESPACE.PROJECTNAME.api.errors :as errors])
  (:import java.util.Base64))

(defn base64-encode
  [s]
  (some->> s
           .getBytes
           (.encodeToString (Base64/getUrlEncoder))))

(defn base64-decode
  [^String s]
  (some->> s
           (.decode (Base64/getUrlDecoder))
           String.))

(defn- decode-jwt
  [encoded-jwt position]
  (some-> (string/split encoded-jwt #"\.")
          position
          (base64-decode)
          (json/parse-string)))

(defn- verify-jwt
  [encoded-jwt]
  (let [decoded-jwt (decode-jwt encoded-jwt first)
        ;;TODO extract to config
        elb-url "https://public-keys.auth.elb.eu-west-2.amazonaws.com/"
        kid (get decoded-jwt "kid")
        ;; obtain public key remotely
        pub-key (-> @(http/get (str elb-url kid))
                    :body
                    bs/to-string
                    k/str->public-key)]
    ;; if claim is corrupted an exception is thrown
    ;; this is hard to test because the token as an
    ;; expiration time of a few seconds
    (jwt/unsign encoded-jwt pub-key {:alg :es256})))

(defn verify-claim
  "Make sure that the claim is sent by our trusted load balancer"
  [headers]
  (try
    (some-> headers
            (get "x-amzn-oidc-data")
            (verify-jwt))
    (catch Exception e :untrusted)))

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

(defn- authenticate
  "Set the request identity (if not already set)"
  [components request]
  (if (nil? (:identity request))
    (add-request-identity {:system components} request)
    request))

(defn wrap-authentication
  [handler components]
  (fn -wrap-authentication
    ([request]
     (handler (authenticate components request)))
    ([request respond raise]
     (handler (authenticate components request) respond raise))))
