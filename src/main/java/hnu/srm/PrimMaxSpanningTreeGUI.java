package hnu.srm;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class PrimMaxSpanningTreeGUI extends JPanel {
    static class Edge {
        int vertex1;
        int vertex2;
        int weight;

        Edge(int vertex1, int vertex2, int weight) {
            this.vertex1 = vertex1;
            this.vertex2 = vertex2;
            this.weight = weight;
        }

        @Override
        public String toString() {
            return "Edge{" +
                    "vertex1=" + vertex1 +
                    ", vertex2=" + vertex2 +
                    ", weight=" + weight +
                    '}';
        }
    }

    private List<Edge> edges;
    private int[][] positions;

    public PrimMaxSpanningTreeGUI(List<Edge> edges, int xNums, int yNums) {
        this.edges = edges;
        // Define positions for the nodes
        positions = new int[xNums * yNums][2];
        int xp=100, yp=100;
        for(int r=0;r<yNums; r++) {
            xp = 100;
            for(int c = 0; c < xNums; c++) {
                positions[r*xNums+c] = new int[]{xp, yp};
                xp += 100;
            }
            yp += 100;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.BLACK);

        // Draw edges
        for (Edge edge : edges) {
            int x1 = positions[edge.vertex1][0];
            int y1 = positions[edge.vertex1][1];
            int x2 = positions[edge.vertex2][0];
            int y2 = positions[edge.vertex2][1];
            g.drawLine(x1, y1, x2, y2);
            g.drawString(String.valueOf(edge.weight), (x1 + x2) / 2, (y1 + y2) / 2);
        }

        // Draw nodes
        for (int i = 0; i < positions.length; i++) {
            int x = positions[i][0];
            int y = positions[i][1];
            g.setColor(Color.RED);
            g.fillOval(x - 10, y - 10, 20, 20);
            g.setColor(Color.BLACK);
            g.drawString(String.valueOf(i), x - 5, y + 5);
        }
    }

    public static List<Edge> primMaxSpanningTree(int n, List<Edge> edges) {
        List<Edge> maxSpanningTree = new ArrayList<>();
        PriorityQueue<Edge> maxHeap = new PriorityQueue<>((a, b) -> b.weight - a.weight);
        boolean[] visited = new boolean[n];
        Map<Integer, List<Edge>> adjacencyList = new HashMap<>();

        // Initialize adjacency list
        for (int i = 0; i < n; i++) {
            adjacencyList.put(i, new ArrayList<>());
        }
        for (Edge edge : edges) {
            adjacencyList.get(edge.vertex1).add(edge);
            adjacencyList.get(edge.vertex2).add(new Edge(edge.vertex2, edge.vertex1, edge.weight));
        }

        // Start from vertex 0
        visited[0] = true;
        maxHeap.addAll(adjacencyList.get(0));

        while (!maxHeap.isEmpty()) {
            Edge edge = maxHeap.poll();
            if (visited[edge.vertex1] && visited[edge.vertex2]) {
                continue;
            }

            maxSpanningTree.add(edge);
            int newVertex = visited[edge.vertex1] ? edge.vertex2 : edge.vertex1;
            visited[newVertex] = true;

            for (Edge e : adjacencyList.get(newVertex)) {
                if (!visited[e.vertex2]) {
                    maxHeap.add(e);
                }
            }
        }

        return maxSpanningTree;
    }

//    public static void main(String[] args) {
//        JFrame frame = new JFrame("Prim's Maximum Spanning Tree");
//        List<Edge> edges = new ArrayList<>();
//        // Add edges to the list
//        edges.add(new Edge(0, 1, 4));
//        edges.add(new Edge(0, 2, 3));
//        edges.add(new Edge(1, 2, 2));
//        edges.add(new Edge(1, 3, 5));
//        edges.add(new Edge(2, 3, 7));
//
//        int n = 4; // Number of vertices
//        List<Edge> maxTree = primMaxSpanningTree(n, edges);
//
//        PrimMaxSpanningTreeGUI panel = new PrimMaxSpanningTreeGUI(maxTree, 2, 2);
//        frame.add(panel);
//        frame.setSize(400, 300);
//        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        frame.setVisible(true);
//    }
}
