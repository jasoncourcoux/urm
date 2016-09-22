(ns app.component
  (:require [reagent.core :as r]
            [app.tetrominoes :refer [tetromino]]))

(defn empty-grid
  [width height]
  (vec (for [rows (range height)]
         (vec (for [cols (range width)]
                [])))))

(defn grid-cell [cell]
  (if (empty? cell) ^{:key (str "cell" (rand-int 999999999))} [:div.grid-cell]
                    ^{:key (str "block" (rand-int 999999999))} [(keyword (str "div.grid-cell.type." (name (first cell))))]))

(defn grid-row [row]
  ^{:key (str "row" (rand-int 999999999))} [:div.grid-row
                                            (for [col row]
                                              (grid-cell col))])

(defn tetromino-cell [x y type]
  ^{:key (str "tetrominocellx" x "y" y)} [(keyword (str "div.tetromino.row" y ".col" x "." (name type)))])

(defn render-current-tetromino
  [state]
  (concat (map-indexed (fn [ri row]
                         (map-indexed (fn [ci col]
                                        (if (= col 1)
                                          (tetromino-cell (+ ci (get-in state [:current-tetromino :x]))
                                                          (+ ri (get-in state [:current-tetromino :y]))
                                                          (get-in state [:current-tetromino :type])))) row)) (get-in state [:current-tetromino :coordinates]))))

(defn grid [game-state]
  (fn []
    (let [state @game-state]
      [:div
       [:div.grid
        (for [row (:grid state)]
          ^{:key (str "row" (rand-int 999999999))} (grid-row row))
        [:div.tetrominoes
         (render-current-tetromino state)]
        (if (= :paused (:state state))
          [:div.paused-overlay
           [:div.paused-text "Paused"]])
        (if (= :game-over (:state state))
          [:div.paused-overlay
           [:div.paused-text "Game over"]])
        [:div.frame]]
       [:div.sidepanel
        [:div.score "Score: " (:score state)]
        [:div.level "Level: " (:level state)]]])))

(defn render [game-state]
  (r/render-component [(grid game-state)]
                      (.getElementById js/document "container")))