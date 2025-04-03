package com.example.projekcik;

import java.util.LinkedList;
import java.util.List;

public class DoubleTwoDimQueue {
    private final List<double[][]> queue;

    public DoubleTwoDimQueue() {
        this.queue = new LinkedList<>();
    }

    public void Qpush(double[][] elem) {
        if (elem == null || elem.length == 0 || elem[0].length < 2) {
            throw new IllegalArgumentException("Invalid input array");
        }

        double[][] localElem = new double[1][2];
        System.arraycopy(elem[0], 0, localElem[0], 0, 2);
        queue.add(localElem);
    }

    public double[][] Qtake() {
        if (queue.isEmpty()) {
            throw new IllegalStateException("Queue is empty");
        }
        return queue.remove(0);
    }

    public double[][] Qpeek(int index) {
        if (index < 0 || index >= queue.size()) {
            throw new IndexOutOfBoundsException("Invalid index: " + index);
        }
        return queue.get(index);
    }

    public double[][] toArray() {
        return toArray(0, queue.size() - 1);
    }

    public double[][] toArray(int start, int end) {
        if (start < 0 || end >= queue.size() || start > end) {
            throw new IndexOutOfBoundsException("Invalid range: " + start + " to " + end);
        }

        double[][] arr = new double[end - start + 1][2];
        for (int i = start, j = 0; i <= end; i++, j++) {
            System.arraycopy(queue.get(i)[0], 0, arr[j], 0, 2);
        }
        return arr;
    }

    public double[] toArray(int start, int end, int index) {
        if (start < 0 || end >= queue.size() || start > end) {
            throw new IndexOutOfBoundsException("Invalid range: " + start + " to " + end);
        }
        if (index < 0 || index > 1) {
            throw new IllegalArgumentException("Index must be 0 or 1");
        }

        double[] arr = new double[end - start + 1];
        for (int i = start, j = 0; i <= end; i++, j++) {
            arr[j] = queue.get(i)[0][index];
        }
        return arr;
    }

    public int getQSize() {
        return queue.size();
    }
}