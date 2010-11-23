import sys, math

def score(weights, features):
    result = 0
    for index, value in features.items():
        if index in weights:
            result += weights[index] * value
    return result

def difference(reference, hypothesis):
    features = reference.copy()
    for index, value in hypothesis.items():
        if index in features:
            features[index] -= value
        else:
            features[index] = -value
    return features

def norm(features):
    result = 0
    for index, value in features.items():
        result += value * value
    return result #math.sqrt(result)

def update(weights, averageWeights, averageWeightFactor, reference, reference_value, reference_score, hypothesis, hypothesis_value, hypothesis_score):
    diff = difference(reference, hypothesis)
    alpha = hypothesis_value - reference_value - (reference_score - hypothesis_score)
    if norm(diff) != 0 and alpha > 0:
        alpha /= norm(diff)
        if alpha > 0.001: alpha = 0.001
        for index, value in diff.items():
            if index in weights:
                weights[index] += alpha * value
                if index not in averageWeights: averageWeights[index] = 0
                averageWeights[index] += averageWeightFactor * alpha * value

if __name__ == '__main__':
    weights = {}
    averageWeights = {}
    labels = {}
    examples = [line.strip().split() for line in sys.stdin.readlines()]

    for example in examples:
        if example[0] not in labels:
            labels[example[0]] = len(labels)
    num_labels = len(labels)

    for iteration in range(10):
        num = 1
        total_loss = 0.0
        for example in examples:
            label = labels[example[0]]
            features = [{} for i in range(num_labels)]
            for token in example[1:]:
                index, value = token.split(":")
                index = int(index)
                value = float(value)
                for i in range(num_labels):
                    features[i][index * num_labels + i] = value
                    if index * num_labels + i not in weights: weights[index * num_labels + i] = 0
            scores = [score(weights, features[i]) for i in range(num_labels)]
            hypothesis = 0
            for i in range(num_labels):
                if scores[i] > scores[hypothesis]: hypothesis = i
            loss = 0
            if label != hypothesis: loss = 1
            total_loss += loss
            averageWeightFactor = (10 * len(examples) - (len(examples) * ((iteration + 1) - 1) + (num + 1)) + 1)
            update(weights, averageWeights, averageWeightFactor, features[label], 0, scores[label], features[hypothesis], loss, scores[hypothesis])
            num += 1
        print >>sys.stderr, "iteration %d: %d examples, avg loss=%f" % (iteration, num, total_loss / num)
        factor = 10.0 * len(examples)
        for index in averageWeights:
            weights[index] = averageWeights[index] / factor

    # save model
    for label, index in labels.items():
        print label, index
    print
    for index, value in weights.items():
        print index, value

