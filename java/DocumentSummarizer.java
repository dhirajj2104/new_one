import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

public class DocumentSummarizer {

    // Load stopwords from a file or define them
    private static Set<String> loadStopWords() {
        // Example stopwords list
        String[] stopWordsArray = {
            "a", "an", "the", "and", "or", "but", "if", "while",
            "with", "is", "in", "at", "of", "on", "for", "to", "from"
            // Add more stopwords as needed
        };
        return new HashSet<>(Arrays.asList(stopWordsArray));
    }

    // Method to read text from different file types
    public static String readFile(String filename) {
        String document = "";
        String filetype = filename.substring(filename.lastIndexOf(".")).toLowerCase();

        try {
            switch (filetype) {
                case ".txt":
                    document = readTextFile(filename);
                    break;
                case ".doc":
                    document = readDocFile(filename);
                    break;
                case ".docx":
                    document = readDocxFile(filename);
                    break;
                case ".pdf":
                    document = readPdfFile(filename);
                    break;
                default:
                    System.out.println("Unsupported file type.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return document;
    }

    private static String readTextFile(String filename) throws IOException {
        StringBuilder content = new StringBuilder();
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        String line;
        while ((line = reader.readLine()) != null) {
            content.append(line).append(" ");
        }
        reader.close();
        return content.toString();
    }

    private static String readDocFile(String filename) throws IOException {
        FileInputStream fis = new FileInputStream(new File(filename));
        HWPFDocument document = new HWPFDocument(fis);
        WordExtractor extractor = new WordExtractor(document);
        String[] paragraphs = extractor.getParagraphText();
        StringBuilder content = new StringBuilder();
        for (String para : paragraphs) {
            content.append(para).append(" ");
        }
        extractor.close();
        fis.close();
        return content.toString();
    }

    private static String readDocxFile(String filename) throws IOException {
        FileInputStream fis = new FileInputStream(new File(filename));
        XWPFDocument document = new XWPFDocument(fis);
        XWPFWordExtractor extractor = new XWPFWordExtractor(document);
        String content = extractor.getText();
        extractor.close();
        fis.close();
        return content;
    }

    private static String readPdfFile(String filename) throws IOException {
        PDDocument document = PDDocument.load(new File(filename));
        PDFTextStripper stripper = new PDFTextStripper();
        String text = stripper.getText(document);
        document.close();
        return text;
    }

    // Preprocess the text: lowercase, remove punctuation, tokenize into sentences
    private static List<String> preprocessText(String text) {
        // Convert to lowercase
        text = text.toLowerCase();

        // Replace newline characters with space
        text = text.replaceAll("\\n", " ");

        // Remove non-alphabetic characters except periods
        text = text.replaceAll("[^a-zA-Z0-9\\.\\s]", "");

        // Split text into sentences using regex
        List<String> sentences = new ArrayList<>();
        Matcher matcher = Pattern.compile("([^\\.]+\\.)").matcher(text);
        while (matcher.find()) {
            String sentence = matcher.group(1).trim();
            if (!sentence.isEmpty()) {
                sentences.add(sentence);
            }
        }

        return sentences;
    }

    // Compute word frequency excluding stopwords
    private static Map<String, Integer> computeWordFrequency(List<String> sentences, Set<String> stopWords) {
        Map<String, Integer> wordFrequency = new HashMap<>();

        for (String sentence : sentences) {
            String[] words = sentence.split("\\s+");
            for (String word : words) {
                if (word.length() > 2 && !stopWords.contains(word)) { // Exclude short words and stopwords
                    wordFrequency.put(word, wordFrequency.getOrDefault(word, 0) + 1);
                }
            }
        }

        return wordFrequency;
    }

    // Score sentences based on word frequency
    private static Map<Integer, Double> scoreSentences(List<String> sentences, Map<String, Integer> wordFrequency) {
        Map<Integer, Double> sentenceScores = new HashMap<>();

        for (int i = 0; i < sentences.size(); i++) {
            String sentence = sentences.get(i);
            String[] words = sentence.split("\\s+");
            double score = 0.0;

            for (String word : words) {
                score += wordFrequency.getOrDefault(word, 0);
            }

            sentenceScores.put(i, score / words.length); // Normalize by sentence length
        }

        return sentenceScores;
    }

    // Generate summary by selecting top N sentences
    private static List<String> generateSummary(List<String> sentences, Map<Integer, Double> sentenceScores, int topN) {
        // Sort sentences by score in descending order
        List<Map.Entry<Integer, Double>> sortedEntries = new ArrayList<>(sentenceScores.entrySet());
        sortedEntries.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        // Select top N sentences
        List<Integer> topSentenceIndices = new ArrayList<>();
        for (int i = 0; i < Math.min(topN, sortedEntries.size()); i++) {
            topSentenceIndices.add(sortedEntries.get(i).getKey());
        }

        // Sort selected sentences by their original order
        Collections.sort(topSentenceIndices);

        // Compile the summary
        List<String> summary = new ArrayList<>();
        for (Integer index : topSentenceIndices) {
            summary.add(sentences.get(index));
        }

        return summary;
    }

    // Main summarization method
    public static String summarizeText(String text, int numSentences) {
        Set<String> stopWords = loadStopWords();
        List<String> sentences = preprocessText(text);

        if (sentences.isEmpty()) {
            return "No valid sentences found in the input text.";
        }

        Map<String, Integer> wordFrequency = computeWordFrequency(sentences, stopWords);
        Map<Integer, Double> sentenceScores = scoreSentences(sentences, wordFrequency);
        List<String> summarySentences = generateSummary(sentences, sentenceScores, numSentences);

        return String.join(" ", summarySentences);
    }

    public static void main(String[] args) {
        // Example usage:
        // Provide the path to your document here
        String filename = "path_to_your_document.pdf"; // Change the path and extension accordingly
        String documentText = readFile(filename);

        // Specify the number of sentences for the summary
        int summaryLength = 5;

        // Generate and print the summary
        String summary = summarizeText(documentText, summaryLength);
        System.out.println("Summary:\n" + summary);
    }
}
