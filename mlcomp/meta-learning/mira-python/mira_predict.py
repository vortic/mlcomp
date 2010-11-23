import sys, math, mira_train

# load model
labels = []
weights = {}
state = 0
for line in open(sys.argv[1]).xreadlines():
    line = line.strip()
    if state == 0:
        if line == "":
            state = 1
        else:
            label, index = line.split()
            index = int(index)
            while len(labels) < index + 1: labels.append(None)
            labels[index] = label
    else:
        index, value = line.split()
        weights[int(index)] = float(value)

num_labels = len(labels)
errors = 0.0
num_examples = 0

for line in sys.stdin:
    example = line.strip().split()
    features = [{} for i in range(num_labels)]
    for token in example[1:]:
        index, value = token.split(":")
        index = int(index)
        value = float(value)
        for i in range(num_labels):
            features[i][index * num_labels + i] = value
    scores = [mira_train.score(weights, features[i]) for i in range(num_labels)]
    hypothesis = 0
    for i in range(num_labels):
        if scores[i] > scores[hypothesis]: hypothesis = i
    if labels[hypothesis] != example[0]:
        errors += 1
    num_examples += 1
    print labels[hypothesis]

print >>sys.stderr, errors / num_examples

