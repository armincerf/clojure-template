(ns PROJECTNAMESPACE.PROJECTNAME.api.crypto
  (:require [buddy.core.codecs :as codecs]
            [buddy.core.nonce :as nonce])
  (:import io.seruco.encoding.base62.Base62
           java.security.SecureRandom))

(def ^:private secure-random (SecureRandom.))

(def ^{:private true
       :tag Base62}
  base62-instance
  "We specifically use the GMP character set so that generated IDs will be roughly
  sorted in byte order."
  (Base62/createInstanceWithGmpCharacterSet))

(defn base62-encode [ba]
  (->> ba
       (.encode base62-instance)
       (codecs/bytes->str)))

(defn random-string
  "Generates an opaque, 16-byte ID without a prefix."
  ([]
   (random-string 16))
  ([num-bytes]
   (-> (nonce/random-nonce num-bytes secure-random)
       (base62-encode))))

(defn uuid [] (str (java.util.UUID/randomUUID)))
