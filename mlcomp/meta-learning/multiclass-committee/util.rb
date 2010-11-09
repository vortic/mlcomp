class Counter < Hash
  def initialize
    super
    super.default = 0
  end

  def +(c)
    tmp = Counter.new()
    c.each do |key, value|
      tmp[key] = self[key] + value
    end
    return tmp
  end

  def *(c)
    #dot product
    sum = 0
    c.each do |key, value|
      sum += self[key] * value
    end
    return sum
  end

  def multiplyAll(scalar)
    self.each_key do |key|
      self[key] *= scalar
    end
  end

  def argMin
    self.to_a.min_by {|a| a[1]}[0]
  end

  def argMax
    self.to_a.max_by {|a| a[1]}[0]
  end
end

