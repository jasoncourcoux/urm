(ns app.component
  (:require [reagent.core :as r]
            [app.tetrominoes :refer [tetromino]]))

(defn grid-cell [cell]
  (if (empty? cell) ^{:key (str "cell" (rand-int 999999999))} [:div.grid-cell]
                    ^{:key (str "block" (rand-int 999999999))} [(keyword (str "div.grid-cell.type." (name (first cell))))]))

(defn grid-row [row]
  ^{:key (str "row" (rand-int 999999999))} [:div.grid-row
                                            (for [col row]
                                              (grid-cell col))])

(defn tetromino-cell [x y type]
  ^{:key (str "tetrominocellx" x "y" y)} [(keyword (str "div.tetromino.row" y ".col" x "." (name type)))])

(defn render-tetromino
  [tetromino]
  (concat (map-indexed (fn [ri row]
                         (map-indexed (fn [ci col]
                                        (if (= col 1)
                                          (tetromino-cell (+ ci (:x tetromino))
                                                          (+ ri (:y tetromino))
                                                          (:type tetromino)))) row)) (:coordinates tetromino))))

(defn grid [game-state]
  (fn []
    (let [state @game-state]
      [:div.game-container
       [:div.grid
        (for [row (:grid state)]
          ^{:key (str "row" (rand-int 999999999))} (grid-row row))
        [:div.tetrominoes
         (render-tetromino (:current-tetromino state))]
        (if (= :paused (:state state))
          [:div.paused-overlay
           [:div.paused-text "Paused"]])
        (if (= :game-over (:state state))
          [:div.paused-overlay
           [:div.paused-text "Game over"]])
        [:div.frame]]
       [:div.sidepanel
        [:div.next-tetrominoes
         (for [t (:next-tetrominoes state)]
           [:div.tetromino-container
            (render-tetromino (assoc t :x 0))])]
        [:div.status
         [:div.score "Score: " (:score state)]
         [:div.level "Level: " (int (Math/floor (/ (:lines-cleared state) 10)))]
         [:div.lines "Lines Cleared: " (:lines-cleared state)]]]])))

(defn render [game-state]
  (r/render-component [(grid game-state)]
                      (.getElementById js/document "container")))