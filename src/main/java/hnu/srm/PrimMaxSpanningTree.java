package hnu.srm;

import java.util.*;

public class PrimMaxSpanningTree {
    static class Edge {
        int vertex;
        int weight;

        Edge(int vertex, int weight) {
            this.vertex = vertex;
            this.weight = weight;
        }
    }

    public static List<Edge> primMaxSpanningTree(Map<Integer, List<Edge>> graph, int start) {
        List<Edge> maxSpanningTree = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();
        PriorityQueue<Edge> maxHeap = new PriorityQueue<>((a, b) -> b.weight - a.weight);

        maxHeap.offer(new Edge(start, Integer.MAX_VALUE));

        while (!maxHeap.isEmpty()) {
            Edge currentEdge = maxHeap.poll();
            int currentVertex = currentEdge.vertex;

            if (visited.contains(currentVertex)) continue;

            visited.add(currentVertex);
            if (currentEdge.weight != Integer.MAX_VALUE) {
                maxSpanningTree.add(currentEdge);
            }

            for (Edge neighbor : graph.getOrDefault(currentVertex, Collections.emptyList())) {
                if (!visited.contains(neighbor.vertex)) {
                    maxHeap.offer(new Edge(neighbor.vertex, neighbor.weight));
                }
            }
        }

        return maxSpanningTree;
    }

    public static void main(String[] args) {
        // 示例图
        Map<Integer, List<Edge>> graph = new HashMap<>();

        graph.computeIfAbsent(0, k -> new ArrayList<>()).add(new Edge(1, 4));
        graph.computeIfAbsent(0, k -> new ArrayList<>()).add(new Edge(2, 3));
        graph.computeIfAbsent(1, k -> new ArrayList<>()).add(new Edge(0, 4));
        graph.computeIfAbsent(1, k -> new ArrayList<>()).add(new Edge(2, 2));
        graph.computeIfAbsent(1, k -> new ArrayList<>()).add(new Edge(3, 5));
        graph.computeIfAbsent(2, k -> new ArrayList<>()).add(new Edge(0, 3));
        graph.computeIfAbsent(2, k -> new ArrayList<>()).add(new Edge(1, 2));
        graph.computeIfAbsent(2, k -> new ArrayList<>()).add(new Edge(3, 7));
        graph.computeIfAbsent(3, k -> new ArrayList<>()).add(new Edge(1, 5));
        graph.computeIfAbsent(3, k -> new ArrayList<>()).add(new Edge(2, 7));

        int startNode = 0;
        List<Edge> maxTree = primMaxSpanningTree(graph, startNode);
        System.out.println("最大生成树的边：");
        for (Edge edge : maxTree) {
            System.out.println("顶点: " + edge.vertex + ", 权重: " + edge.weight);
        }
    }
}
