;; Copyright (C) 2022-2023 Magenta Health Inc. 
;; Authored by Carmen La <https://carmen.la/>.

;; This file is part of LookupLab-UI.

;; LookupLab-UI is free software: you can redistribute it and/or modify
;; it under the terms of the GNU Affero General Public License as
;; published by the Free Software Foundation, either version 3 of the
;; License, or (at your option) any later version.

;; LookupLab-UI is distributed in the hope that it will be useful,
;; but WITHOUT ANY WARRANTY; without even the implied warranty of
;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
;; GNU Affero General Public License for more details.

;; You should have received a copy of the GNU Affero General Public License
;; along with LookupLab-UI.  If not, see <https://www.gnu.org/licenses/>.

(ns user
  (:require
   [shadow.cljs.devtools.api :as shadow]
   [ring.middleware.resource :refer [wrap-resource]]))

(def app (wrap-resource identity "public"))

(defn cljs []
  (shadow/repl :app))
