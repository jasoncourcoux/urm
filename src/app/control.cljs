(ns app.control
  (:require [app.tetrominoes :refer [rotate-tetromino tetrominoes]]))

(defn tetromino->coords
  [t]
  (filter (comp not nil?)
          (mapcat (fn [row y-coord]
                    (map (fn [col x-coord]
                           (if (= 1 col) [x-coord y-coord])) row (iterate inc (:x t))))
                  (:coordinates t) (iterate inc (:y t)))))

(defn can-move-down?
  [state]
  (let [t (:current-tetromino state)
        g (:grid state)
        coords (tetromino->coords t)]
    (every? true? (map (fn [[col row]]
                         (cond (>= (inc row) (count g)) false
                               (empty? (get-in g [(inc row) col])) true
                               :else false)) coords))))

(defn can-move-left?
  [state]
  (let [t (:current-tetromino state)
        g (:grid state)
        coords (tetromino->coords t)]
    (every? true? (map (fn [[col row]]
                         (cond (< (dec col) 0) false
                               (empty? (get-in g [row (dec col)])) true
                               :else false)) coords))))

(defn can-move-right?
  [state]
  (let [t (:current-tetromino state)
        g (:grid state)
        coords (tetromino->coords t)]
    (every? true? (map (fn [[col row]]
                         (cond (>= (inc col) (:grid-width state)) false
                               (empty? (get-in g [row (inc col)])) true
                               :else false)) coords))))

(defn can-rotate?
  [state]
  (let [updated-coordinates (-> state :current-tetromino rotate-tetromino tetromino->coords)]
        (every? true? (map (fn [[col row]]
                             (cond (>= col (:grid-width state)) false
                                   (<  col 0) false
                                   (>= row (count (:grid state))) false
                                   (empty? (get-in state [:grid row col])) true
                                   :else false)) updated-coordinates))))

(defn move-left
  [state]
  (if (can-move-left? state) (update-in state [:current-tetromino :x] dec)
                             state))

(defn move-right
  [state]
  (if (can-move-right? state) (update-in state [:current-tetromino :x] inc)
                              state))

(defn rotate
  [state]
  (if (can-rotate? state) (update-in state [:current-tetromino] rotate-tetromino)
                          state))

(defn merge-tetromino-into-grid [state]
  (let [type (:type (:current-tetromino state))
        coordinates (tetromino->coords (:current-tetromino state))]
  (reduce (fn [s [col row]]
            (assoc-in s [:grid row col] [type])) state coordinates)))

(defn move-down
  [state]
  (if (can-move-down? state) (update-in state [:current-tetromino :y] inc)
                             (-> state
                                 merge-tetromino-into-grid
                                 (assoc :current-tetromino (first (:next-tetrominoes state))
                                        :next-tetrominoes (conj (rest (:next-tetrominoes state)) (first (tetrominoes)))))))
(defn toggle-pause
  [state]
  (case (:state state)
    :running (assoc state :state :paused)
    :paused (assoc state :state :running)))

(defn bind-key-handlers
  [game-state]
  (.addEventListener js/window
                     "keydown"
                     (fn [e]
                       (let [code (.-keyCode e)
                             LEFT 37 RIGHT 39 UP 38 DOWN 40 SPACE 32
                             allowed-codes [LEFT RIGHT UP DOWN SPACE]]
                         (if (some #{code} allowed-codes)
                           (do (.preventDefault e)
                               (if (= :running (:state @game-state))
                                 (cond
                                   (= code LEFT) (swap! game-state move-left)
                                   (= code RIGHT) (swap! game-state move-right)
                                   (= code UP) (swap! game-state rotate)
                                   (= code DOWN) (swap! game-state move-down)
                                   (= code SPACE) (swap! game-state toggle-pause))
                                 (if (= code SPACE) (swap! game-state toggle-pause)))))))))
