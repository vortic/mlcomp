import sys
import util

class Winnow:
    def __init__(self):
        self.weights = {}
        self.legalLabels = []
        self.alpha = 2

        self.legalLabels = [1,]
        for l in self.legalLabels:
            self.weights[l] = util.WinnowCounter()

    def classify(self, data):
        vectors = util.Counter()
        for l in self.legalLabels:
            vectors[l] = self.weights[l] * data
        vectors.normalize()
        return vectors

    def update(self, label, guess, data):
        if guess != label:
            if not label in self.legalLabels:
                self.legalLabels.append(label)
                self.weights[label] = util.WinnowCounter()
            self.weights[label].divideAll(self.alpha)
            self.weights[guess].divideAll(float(1)/self.alpha)
