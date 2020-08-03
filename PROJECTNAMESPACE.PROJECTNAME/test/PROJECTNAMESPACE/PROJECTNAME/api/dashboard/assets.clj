(ns PROJECTNAMESPACE.PROJECTNAME.api.dashboard.assets
  (:require [PROJECTNAMESPACE.PROJECTNAME.api.dashboard.assets.routes :as assets.routes]
            [PROJECTNAMESPACE.PROJECTNAME.api.dashboard.assets.domain :as assets.domain]
            [PROJECTNAMESPACE.test.utils :as utils]
            [PROJECTNAMESPACE.PROJECTNAME.api.fixtures :as f]
            [clojure.spec.alpha :as s]
            [clojure.test :as t]))

(t/use-fixtures :once f/with-system)

(t/deftest assets-crud-test
  (let [assets-resp (utils/request-to ::assets.routes/assets-collection
                                      {:request-method :get})
        assets (:data (:body assets-resp))
        id (:id (first assets))
        new-asset {:id "hi"
                   :customer :customer/a123
                   :name "Work email"
                   :data "example@email.com"
                   :type :email}]
    (t/testing "Get all assets returns valid body"
      (t/is (nil? (s/explain-data  ::assets.domain/asset (first assets)))))
    (t/testing "Get asset by id returns correct asset"
      (let [resp (utils/request-to ::assets.routes/asset-resource
                                   {:request-method :get
                                    :path-params {:id (name id)}})]
        (t/is (nil? (s/explain-data  ::assets.domain/asset-ext
                                     (:data (:body resp)))))))
    (t/testing "Can update an asset"
      (let [id (:id (first assets))
            resp (utils/request-to ::assets.routes/asset-resource
                                   {:request-method :put
                                    :path-params {:id (name id)}
                                    :body-params {:asset new-asset}})]
        (t/is (nil? (s/explain-data  ::assets.domain/asset
                                     (:data (:body resp)))))))))

