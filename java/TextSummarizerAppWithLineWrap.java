import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

public class TextSummarizerAppWithLineWrap {

    private JFrame frame;
    private JTextArea originalTextArea;
    private JTextArea summaryTextArea;
    private JLabel statusLabel;
    private JButton summarizeButton;
    private JButton openFileButton;
    private JSpinner summaryLengthSpinner;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TextSummarizerAppWithLineWrap().createAndShowGUI());
    }

    private void createAndShowGUI() {
        frame = new JFrame("Text Summarizer with Line Wrap");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 800);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout());

        // Custom font
        Font customFont = new Font("Segoe UI", Font.PLAIN, 14);

        // Create text areas for original and summary
        originalTextArea = new JTextArea();
        originalTextArea.setEditable(true); // Allow editing of the text
        originalTextArea.setFont(customFont);
        originalTextArea.setBackground(Color.WHITE);
        originalTextArea.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        originalTextArea.setMargin(new Insets(10, 10, 10, 10)); // Set margin inside the original text area
        originalTextArea.setLineWrap(true); // Enable line wrapping
        originalTextArea.setWrapStyleWord(true); // Wrap at word boundaries

        summaryTextArea = new JTextArea();
        summaryTextArea.setEditable(false); // Summary area is not editable
        summaryTextArea.setFont(customFont);
        summaryTextArea.setBackground(Color.WHITE);
        summaryTextArea.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        summaryTextArea.setMargin(new Insets(10, 10, 10, 10)); // Set margin inside the summary text area
        summaryTextArea.setLineWrap(true); // Enable line wrapping
        summaryTextArea.setWrapStyleWord(true); // Wrap at word boundaries

        // Status label
        statusLabel = new JLabel("Write or edit text, then summarize", JLabel.CENTER);
        statusLabel.setFont(customFont.deriveFont(Font.BOLD));
        statusLabel.setForeground(Color.DARK_GRAY);

        // Open File button
        openFileButton = new JButton("Open File");
        openFileButton.setFont(customFont.deriveFont(Font.BOLD));
        openFileButton.setBackground(new Color(70, 130, 180)); // Steel Blue
        openFileButton.setForeground(Color.WHITE);
        openFileButton.setFocusPainted(false);
        openFileButton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        openFileButton.addActionListener(new OpenFileAction());

        // Summarize button
        summarizeButton = new JButton("Summarize Text");
        summarizeButton.setFont(customFont.deriveFont(Font.BOLD));
        summarizeButton.setBackground(new Color(70, 130, 180)); // Steel Blue
        summarizeButton.setForeground(Color.WHITE);
        summarizeButton.setFocusPainted(false);
        summarizeButton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        summarizeButton.addActionListener(new SummarizeTextAction());

        // Summary Length Spinner
        summaryLengthSpinner = new JSpinner(new SpinnerNumberModel(2, 1, 10, 1));
        summaryLengthSpinner.setFont(customFont);
        summaryLengthSpinner.setToolTipText("Select number of summary sentences");

        // Label for spinner
        JLabel spinnerLabel = new JLabel("Summary Length:");
        spinnerLabel.setFont(customFont);

        // Layout for controls
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BorderLayout(10, 0));
        JPanel spinnerPanel = new JPanel();
        spinnerPanel.add(spinnerLabel);
        spinnerPanel.add(summaryLengthSpinner);
        controlPanel.add(spinnerPanel, BorderLayout.WEST);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(openFileButton);
        buttonPanel.add(summarizeButton);
        controlPanel.add(buttonPanel, BorderLayout.EAST);
        controlPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Add scroll panes for the text areas
        JScrollPane originalScrollPane = new JScrollPane(originalTextArea);
        originalScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        originalScrollPane.setBorder(BorderFactory.createTitledBorder("Original Text"));

        JScrollPane summaryScrollPane = new JScrollPane(summaryTextArea);
        summaryScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        summaryScrollPane.setBorder(BorderFactory.createTitledBorder("Summary"));

        // Layout for text areas using JSplitPane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, originalScrollPane, summaryScrollPane);
        splitPane.setResizeWeight(0.5);
        splitPane.setDividerLocation(frame.getWidth() / 2);
        splitPane.setContinuousLayout(true);
        splitPane.setOneTouchExpandable(true);

        // Add components to frame
        frame.add(controlPanel, BorderLayout.NORTH);
        frame.add(splitPane, BorderLayout.CENTER);
        frame.add(statusLabel, BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    // Action listener for opening a file
    private class OpenFileAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            JFileChooser fileChooser = new JFileChooser();
            int returnValue = fileChooser.showOpenDialog(frame);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                try {
                    statusLabel.setText("Processing file: " + selectedFile.getName());
                    String text = readFile(selectedFile);
                    originalTextArea.setText(text); // Load file content into the original text area
                    statusLabel.setText("File loaded successfully.");
                } catch (IOException ioException) {
                    statusLabel.setText("Error reading file: " + ioException.getMessage());
                } catch (Exception exception) {
                    statusLabel.setText("Error: " + exception.getMessage());
                }
            }
        }
    }

    // Action listener for summarizing text
    private class SummarizeTextAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String originalText = originalTextArea.getText(); // Get the text from the original text area
            if (originalText.isEmpty()) {
                statusLabel.setText("Please enter or load some text to summarize.");
                return;
            }

            int summaryLength = (Integer) summaryLengthSpinner.getValue();
            summarizeButton.setEnabled(false);
            statusLabel.setText("Summarizing...");

            // Perform summarization in a separate thread to keep UI responsive
            new Thread(() -> {
                String summary = summarize(originalText, summaryLength);
                SwingUtilities.invokeLater(() -> {
                    summaryTextArea.setText(summary);
                    statusLabel.setText("Text summarized successfully.");
                    summarizeButton.setEnabled(true);
                });
            }).start();
        }
    }

    // Method to read file content
    private String readFile(File file) throws IOException {
        StringBuilder text = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                text.append(line).append(" ");
            }
        }
        return text.toString().trim();
    }

    // Summarization method with optimizations
    private String summarize(String text, int summarySentencesCount) {
        if (text == null || text.trim().isEmpty()) return "";

        String[] sentences = SENTENCE_SPLIT_PATTERN.split(text);
        if (sentences.length == 0) return "";

        // Initialize word frequency map
        Map<String, Integer> wordFrequency = new HashMap<>();

        // First pass: Calculate word frequencies
        for (String sentence : sentences) {
            String cleaned = cleanText(sentence);
            if (cleaned.isEmpty()) continue;
            String[] words = cleaned.split(WORD_SPLIT_PATTERN);
            for (String word : words) {
                if (word.isEmpty()) continue;
                wordFrequency.put(word, wordFrequency.getOrDefault(word, 0) + 1);
            }
        }

        // Second pass: Score sentences
        double[] sentenceScores = new double[sentences.length];
        for (int i = 0; i < sentences.length; i++) {
            String cleaned = cleanText(sentences[i]);
            if (cleaned.isEmpty()) continue;
            String[] words = cleaned.split(WORD_SPLIT_PATTERN);
            double score = 0.0;
            for (String word : words) {
                if (word.isEmpty()) continue;
                score += wordFrequency.getOrDefault(word, 0);
            }
            sentenceScores[i] = score;
        }

        // Pair sentences with scores and indices
        List<SentenceScore> scoredSentences = new ArrayList<>();
        for (int i = 0; i < sentences.length; i++) {
            scoredSentences.add(new SentenceScore(sentences[i], sentenceScores[i], i));
        }

        // Use a priority queue to keep top N sentences
        PriorityQueue<SentenceScore> pq = new PriorityQueue<>(summarySentencesCount, Comparator.comparingDouble(a -> a.score));
        for (SentenceScore ss : scoredSentences) {
            if (pq.size() < summarySentencesCount) {
                pq.offer(ss);
            } else if (ss.score > pq.peek().score) {
                pq.poll();
                pq.offer(ss);
            }
        }

        // Extract top sentences and sort them by original order
        List<SentenceScore> topSentences = new ArrayList<>(pq);
        topSentences.sort(Comparator.comparingInt(a -> a.index));

        // Build the summary
        StringBuilder summary = new StringBuilder();
        for (SentenceScore ss : topSentences) {
            summary.append(ss.sentence).append(" ");
        }

        return summary.toString().trim();
    }

    // Efficient text cleaning without regex
    private String cleanText(String text) {
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (Character.isLetter(c) || Character.isWhitespace(c)) {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
    }

    // Helper class to store sentence information
    private static class SentenceScore {
        String sentence;
        double score;
        int index;

        SentenceScore(String sentence, double score, int index) {
            this.sentence = sentence;
            this.score = score;
            this.index = index;
        }
    }

    // Precompile patterns as static final variables for efficiency
    private static final Pattern SENTENCE_SPLIT_PATTERN = Pattern.compile("(?<=[.!?])\\s+");
    private static final String WORD_SPLIT_PATTERN = "\\s+";
}
