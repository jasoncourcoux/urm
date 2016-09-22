(ns app.tetrominoes)

(def tetromino-types [:l :i :j :t :s :z :o])

(defn bounding-square
  [size]
  (let [row (vec (take size (repeat 0)))]
    (vec (take size (repeat row)))))

(defn create-tetromino [bounding-box-size & active-coordinates]
  (reduce (fn [box coords]
            (assoc-in box coords 1))
          (bounding-square bounding-box-size)
          active-coordinates))

(defn tetromino [type]
  (let [coordinates (case type
                      :i (create-tetromino 4 [1 0] [1 1] [1 2] [1 3])
                      :j (create-tetromino 3 [0 0] [1 0] [1 1] [1 2])
                      :l (create-tetromino 3 [0 2] [1 0] [1 1] [1 2])
                      :t (create-tetromino 3 [0 1] [1 0] [1 1] [1 2])
                      :s (create-tetromino 3 [0 1] [0 2] [1 0] [1 1])
                      :z (create-tetromino 3 [0 0] [0 1] [1 1] [1 2])
                      :o (create-tetromino 2 [0 0] [0 1] [1 0] [1 1]))]
    {:type        type
     :coordinates coordinates
     :x           (if (= type :o) 5 4)                      ; i and o spawn in middle columns, others just left of middle
     :y           0}))

(defn tetrominoes []
  (let [types (repeatedly (fn [] (rand-nth tetromino-types)))]
    (map tetromino types)))

(defn rotate-coordinates [coordinates]
  (vec (for [n (range (count coordinates))]
         (vec (reverse (map #(nth % n) coordinates))))))

(defn rotate-tetromino [t]
  (update t :coordinates rotate-coordinates))