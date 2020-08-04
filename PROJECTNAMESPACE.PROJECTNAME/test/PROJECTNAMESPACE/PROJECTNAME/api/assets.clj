(ns PROJECTNAMESPACE.PROJECTNAME.api.assets
  (:require [PROJECTNAMESPACE.PROJECTNAME.api.assets.routes :as assets.routes]
            [PROJECTNAMESPACE.PROJECTNAME.api.assets.domain :as assets.domain]
            [PROJECTNAMESPACE.PROJECTNAME.common :as common]
            [PROJECTNAMESPACE.test.utils :as utils]
            [PROJECTNAMESPACE.PROJECTNAME.api.fixtures :as f]
            [PROJECTNAMESPACE.PROJECTNAME.api.ids :as ids]
            [clojure.spec.alpha :as s]
            [clojure.test :as t]))

(t/use-fixtures :once f/with-system)

(t/deftest assets-crud-test
  (let [assets-resp (utils/request-to ::assets.routes/assets-collection
                                      {:request-method :get})
        assets (:data (:body assets-resp))
        id (:id (first assets))
        new-data "imnew@email.com"
        updated-asset (assoc (first assets) :asset/data new-data)]
    (t/testing "Get all assets returns valid body"
      (t/is (nil? (s/explain-data  ::assets.domain/asset-ext (first assets)))))
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
                                    :body-params {:asset updated-asset}})]
        (t/testing "Adding a valid asset is successful"
          (t/is (utils/submap? {:status 200
                                :body
                                {:data
                                 {:asset/data new-data}}} resp))
          (t/is (nil? (s/explain-data  ::assets.domain/asset-ext
                                       (:data (:body resp))))))
        (t/testing "Adding an asset with a non existant customer is unsuccessful"
          (t/is (utils/submap?
                 {:status 400}
                 (utils/request-to ::assets.routes/asset-resource
                                   {:request-method :put
                                    :path-params {:id (name id)}
                                    :body-params
                                    {:asset (assoc updated-asset
                                                   :asset/customer
                                                   :customer/a123)}}))))))
    (t/testing "Can add a new asset"
      (let [old-asset (first assets)
            new-asset-name "new asset"
            new-asset (assoc old-asset :asset/name new-asset-name)
            add-asset-resp (utils/request-to ::assets.routes/assets-collection
                                             {:request-method :post
                                              :body-params {:asset new-asset}})
            new-id (get-in add-asset-resp [:body :data :id])
            find-asset-resp (utils/request-to ::assets.routes/asset-resource
                                             {:request-method :get
                                              :path-params {:id (common/id-key new-id)}})]
        (t/testing "Adding a valid asset is successful"
          (t/is (utils/submap? {:status 200
                                :body
                                {:data
                                 {:id new-id}}} add-asset-resp))
          (t/is (nil? (s/explain-data  ::assets.domain/asset-ext
                                       (:data (:body add-asset-resp))))))
        (t/testing "Can find new asset"
          (prn new-id new-asset)
          (t/is (utils/submap? {:status 200
                                :body
                                {:data
                                 {:asset/name new-asset-name
                                  :id new-id}}} find-asset-resp))
          (t/is (nil? (s/explain-data  ::assets.domain/asset-ext
                                       (:data (:body find-asset-resp))))))))))

