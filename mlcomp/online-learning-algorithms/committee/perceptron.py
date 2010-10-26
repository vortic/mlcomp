import sys
import util

class Perceptron:
    def __init__(self):
        self.weights = {}
        self.legalLabels = []

        self.legalLabels = [1,]
        for l in self.legalLabels:
            self.weights[l] = util.Counter()

        self.numupdates = 0
        self.learningrate = 1

    def classify(self, data):
        vectors = util.Counter()
        for l in self.legalLabels:
            vectors[l] = self.weights[l] * data
        vectors.normalize()
        return vectors

    def update(self, label, guess, data):
        self.numupdates += 1
        self.learningrate = float(1)/(self.numupdates**.5)
        if guess != label:
            if not label in self.legalLabels:
                self.legalLabels.append(label)
                self.weights[label] = util.Counter()
            data.divideAll(1/float(self.learningrate))
            self.weights[label] += data
            self.weights[guess] -= data
