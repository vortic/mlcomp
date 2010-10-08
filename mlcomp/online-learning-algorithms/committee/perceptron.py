import sys
import util

class Perceptron:
    def __init__(self):
        self.weights = {}
        self.legalLabels = []

        self.legalLabels = [1,]
        for l in self.legalLabels:
            self.weights[l] = util.Counter()

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
                self.weights[label] = util.Counter()
            self.weights[label] += data
            self.weights[guess] -= data
