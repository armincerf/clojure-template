(ns PROJECTNAMESPACE.PROJECTNAME.api.auth
  (:require [aleph.http :as http]
            [buddy.core.keys :as k]
            [buddy.sign.jwt :as jwt]
            [byte-streams :as bs]
            [cheshire.core :as json]
            [clojure.string :as string]
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
  [components request]
  (let [claim (if (nil? (:auth-method components))
                {:name "John Doe"}
                (verify-claim (:headers request)))]
    (if-let [name (:name claim)]
      (assoc request :identity claim)
      (throw (errors/exception :unauthorized {:auth-method "cognito"
                                              :claim claim})))))

(defn- authenticate
  "Set the request identity (if not already set) from cognito jwt"
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
