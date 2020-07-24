(ns PROJECTNAMESPACE.PROJECTNAME.api.submit-test
  (:require [clojure.test :as t]
            [PROJECTNAMESPACE.PROJECTNAME.api.routes :as routes]
            [PROJECTNAMESPACE.PROJECTNAME.api.fixtures :as f]
            [PROJECTNAMESPACE.test.utils :as utils]))

(t/use-fixtures :once f/with-system)

(t/deftest sample-test
  (t/testing "api request"
    (let [resp (utils/request-to ::routes/me {:request-method :get})]
      (t/is (utils/submap? {:status 401} resp)))))
