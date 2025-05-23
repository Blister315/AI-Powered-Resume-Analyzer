import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.text.PDFTextStripper;
import org.json.JSONObject;
import org.json.JSONArray;
import java.nio.charset.StandardCharsets;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.geom.Arc2D;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class AIResumeAnalyzer {

    private JTextArea resumeResultText;
    private DualScorePanel dualScorePanel;
    private JTextArea chatbotArea;
    private JTextField chatbotInput;
    private File selectedFile;
    private List<File> uploadedResumes = new ArrayList<>();
    private Map<File, String> resumeTexts = new HashMap<>();
    private Map<File, String> analysisResults = new HashMap<>();
    private Map<File, Integer> atsScores = new HashMap<>();
    private List<Entry<File, Integer>> rankedResumes = new ArrayList<>();
    private int numberOfResumesToUpload = 0;
    private int resumesUploadedCount = 0;
    private JButton compareButton;
    private JButton uploadButton;
    private JButton analyzeButton;
    private JButton downloadAnalysisButton;
    private JButton sendButton;
    private JTextArea grammarAndLanguageQualityText;

    private static final String COHERE_API_KEY = "your-cohere-ai-api-key";
    private static final String COHERE_API_URL = "https://api.cohere.ai/v1/generate";
    private static final String COHERE_CHAT_URL = "https://api.cohere.ai/v1/chat";

    // Refined Color Palette
    private static final Color PRIMARY_COLOR = new Color(70, 130, 180);
    private static final Color SECONDARY_COLOR = new Color(240, 128, 128);
    private static final Color ACCENT_COLOR = new Color(255, 215, 0);
    private static final Color BACKGROUND_COLOR = new Color(255, 250, 240);
    private static final Color TEXT_COLOR = new Color(51, 51, 51);
    private static final Font DEFAULT_FONT = new Font("Arial", Font.PLAIN, 14);
    private static final Font RESULT_FONT = new Font("Arial", Font.PLAIN, 16);

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            AIResumeAnalyzer analyzer = new AIResumeAnalyzer();
            analyzer.createAndShowGUI();
        });
    }

    public void createAndShowGUI() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (UnsupportedLookAndFeelException | ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }

        JFrame frame = new JFrame("AI Resume Analyzer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1100, 700); // Adjusted width for three equal sections
        frame.setLayout(new BorderLayout(15, 15));
        frame.getContentPane().setBackground(BACKGROUND_COLOR);

        // --- Top Panel ---
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        topPanel.setBackground(BACKGROUND_COLOR);
        topPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        uploadButton = createStyledButton("Upload Resumes (PDF)");
        analyzeButton = createStyledButton("Analyze Resume");
        compareButton = createStyledButton("Compare Resumes");
        compareButton.setEnabled(false);

        topPanel.add(uploadButton);
        topPanel.add(analyzeButton);
        topPanel.add(compareButton);

        // --- Center Panel ---
        JPanel centerPanel = new JPanel(new GridLayout(1, 3, 15, 15)); // GridLayout with 3 columns
        centerPanel.setBackground(BACKGROUND_COLOR);
        centerPanel.setBorder(new EmptyBorder(0, 15, 15, 15));

        // Analysis Result Panel
        JPanel analysisPanel = new JPanel(new BorderLayout());
        analysisPanel.setBorder(BorderFactory.createTitledBorder(new LineBorder(PRIMARY_COLOR, 2), "Analysis Result",
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                DEFAULT_FONT, PRIMARY_COLOR));
        dualScorePanel = new DualScorePanel();
        dualScorePanel.setPreferredSize(new Dimension(200, 200));
        analysisPanel.add(dualScorePanel, BorderLayout.NORTH);
        resumeResultText = createStyledTextArea(RESULT_FONT);
        JScrollPane resumeScrollPane = new JScrollPane(resumeResultText);
        analysisPanel.add(resumeScrollPane, BorderLayout.CENTER);
        centerPanel.add(analysisPanel);

        // Chatbot Panel
        chatbotArea = createStyledTextArea();
        JScrollPane chatbotScrollPane = createStyledScrollPane(chatbotArea, "Chatbot");
        centerPanel.add(chatbotScrollPane);

        // Grammar and Language Quality Panel
        grammarAndLanguageQualityText = createStyledTextArea();
        JScrollPane grammarScrollPane = createStyledScrollPane(grammarAndLanguageQualityText, "Grammar Quality");
        centerPanel.add(grammarScrollPane);

        // --- Bottom Panel ---
        // --- Bottom Panel ---
        JPanel bottomPanel = new JPanel(new BorderLayout(15, 15));
        bottomPanel.setBackground(BACKGROUND_COLOR);
        bottomPanel.setBorder(new EmptyBorder(0, 15, 15, 15));

        chatbotInput = createStyledTextField();
        sendButton = createStyledButton("Send");
        downloadAnalysisButton = createStyledButton("Download Analysis (as PDF)");

        JPanel chatInputPanel = new JPanel(new BorderLayout(10, 0));
        chatInputPanel.setBackground(BACKGROUND_COLOR);
        chatInputPanel.add(chatbotInput, BorderLayout.CENTER);
        chatInputPanel.add(sendButton, BorderLayout.EAST);

        bottomPanel.add(chatInputPanel, BorderLayout.NORTH);
        bottomPanel.add(downloadAnalysisButton, BorderLayout.SOUTH);

        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(centerPanel, BorderLayout.CENTER);
        frame.add(bottomPanel, BorderLayout.SOUTH);

        frame.setVisible(true);

        // Action Listeners (Modified to update new UI components)
        uploadButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            int returnVal = fileChooser.showOpenDialog(frame);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                selectedFile = fileChooser.getSelectedFile();
                uploadedResumes.add(selectedFile);
                try {
                    String text = extractTextFromPDF(selectedFile);
                    resumeTexts.put(selectedFile, text);
                    resumeResultText.append("Uploaded: " + selectedFile.getName() + "\n");
                    grammarAndLanguageQualityText.append("Uploaded: " + selectedFile.getName() + "\n"); // Append to Grammar Quality
                    if (uploadedResumes.size() >= 2) {
                        compareButton.setEnabled(true);
                    }
                } catch (IOException ex) {
                    resumeResultText.append("Error reading the PDF file: " + selectedFile.getName() + "\n");
                    grammarAndLanguageQualityText.append("Error reading the PDF file: " + selectedFile.getName() + "\n"); // Append error to Grammar Quality
                    ex.printStackTrace();
                }
            }
        });

        analyzeButton.addActionListener(e -> {
            if (uploadedResumes.size() >= 1) {
                File resumeToAnalyze = uploadedResumes.get(uploadedResumes.size() - 1);
                String textToAnalyze = resumeTexts.get(resumeToAnalyze);
                resumeResultText.setText("Analyzing: " + resumeToAnalyze.getName() + "\n");
                generateAIAnalysis(textToAnalyze);
                predictInterviewProbability(textToAnalyze);
            } else {
                JOptionPane.showMessageDialog(frame, "Please upload at least one resume to analyze.", "Upload Required", JOptionPane.WARNING_MESSAGE);
            }
        });

        compareButton.addActionListener(e -> {
            if (uploadedResumes.size() >= 2) {
                String numResumesStr = JOptionPane.showInputDialog(frame, "Enter the number of resumes to compare (minimum 2):");
                try {
                    numberOfResumesToUpload = Integer.parseInt(numResumesStr);
                    if (numberOfResumesToUpload >= 2 && numberOfResumesToUpload <= uploadedResumes.size()) {
                        performResumeComparison();
                    } else {
                        JOptionPane.showMessageDialog(frame, "Please enter a valid number of resumes (minimum 2, and not more than uploaded).", "Input Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(frame, "Invalid input. Please enter a number.", "Input Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(frame, "Please upload at least two resumes to use the compare feature.", "Upload Required", JOptionPane.WARNING_MESSAGE);
            }
        });

        downloadAnalysisButton.addActionListener(e -> {
            String analysisText = resumeResultText.getText() + "\n\nATS Compatibility Score: " + dualScorePanel.getAtsScore() + "%\nInterview Probability: " + dualScorePanel.getInterviewProbability() + "%\n\n" +
                    "Grammar Quality:\n" + grammarAndLanguageQualityText.getText() + "\n\n" +
                    "Chatbot Interactions:\n" + chatbotArea.getText(); // Include chatbot text

            System.out.println("Text to PDF:\n" + analysisText);
            if (!analysisText.isEmpty()) {
                try {
                    JFileChooser fileChooser = new JFileChooser();
                    fileChooser.setDialogTitle("Save Analysis as PDF");
                    FileNameExtensionFilter filter = new FileNameExtensionFilter("PDF files (*.pdf)", "pdf");
                    fileChooser.setFileFilter(filter);

                    int userSelection = fileChooser.showSaveDialog(frame);

                    if (userSelection == JFileChooser.APPROVE_OPTION) {
                        File fileToSave = fileChooser.getSelectedFile();
                        if (!fileToSave.getName().toLowerCase().endsWith(".pdf")) {
                            fileToSave = new File(fileToSave.getAbsolutePath() + ".pdf");
                        }
                        generatePdfFromText(analysisText, fileToSave);
                        JOptionPane.showMessageDialog(frame, "Analysis saved to: " + fileToSave.getAbsolutePath(), "Download Successful", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(frame, "Error saving analysis to PDF: " + ex.getMessage(), "Download Error", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                }
            } else {
                JOptionPane.showMessageDialog(frame, "No analysis to download.", "Download Error", JOptionPane.WARNING_MESSAGE);
            }
        });

        sendButton.addActionListener(e -> {
            String userInput = chatbotInput.getText();
            if (!userInput.isEmpty() && selectedFile != null) {
                chatbotArea.append("You: " + userInput + "\n");
                chatbotInput.setText("");
                try {
                    String resumeText = extractTextFromPDF(selectedFile);
                    List<String> aiResponseList = callAI("Analyze chat request related to the resume:", userInput + "\n\n" + resumeText);
                    if (aiResponseList != null && !aiResponseList.isEmpty()) {
                        String aiResponse = aiResponseList.get(0).trim();
                        chatbotArea.append("AI: " + aiResponse + "\n");
                    } else {
                        chatbotArea.append("AI: No response received.\n");
                    }
                } catch (IOException ex) {
                    chatbotArea.append("Error reading the PDF file.\n");
                    ex.printStackTrace();
                } catch (Exception ex) {
                    chatbotArea.append("Error communicating with AI for chat.\n");
                    ex.printStackTrace();
                }
            } else if (selectedFile == null) {
                chatbotArea.append("Please upload a resume first to use the chatbot.\n");
            } else {
                chatbotArea.append("Please enter a message.\n");
            }
        });

        chatbotInput.addActionListener(e -> sendButton.doClick());
    }

    private void predictInterviewProbability(String resumeText) {
        try {
            String prompt = "Estimate the probability (as a percentage) of this resume leading to an interview, considering its overall quality, ATS compatibility, skills match, and presentation.\n\n" +
                    "RESUME:\n" + resumeText.substring(0, Math.min(4000, resumeText.length())) + "\n\n" +
                    "Provide the probability as: Interview Probability: [Percentage]%";

            String aiResponse = callCohereAPI(prompt);
            Pattern pattern = Pattern.compile("Interview Probability: (\\d+)%");
            Matcher matcher = pattern.matcher(aiResponse);
            if (matcher.find()) {
                try {
                    int probability = Integer.parseInt(matcher.group(1));
                    dualScorePanel.setInterviewProbability(probability);
                } catch (NumberFormatException e) {
                    System.err.println("Could not parse interview probability: " + matcher.group(1));
                    dualScorePanel.setInterviewProbability(0);
                }
            } else {
                dualScorePanel.setInterviewProbability(0);
            }
        } catch (Exception e) {
            dualScorePanel.setInterviewProbability(0);
            e.printStackTrace();
        }
    }

    // --- Styling Helper Methods ---
    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setFont(DEFAULT_FONT);
        button.setBackground(PRIMARY_COLOR);
        button.setForeground(TEXT_COLOR);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        Border lineBorder = new LineBorder(PRIMARY_COLOR, 2);
        Border emptyBorder = new EmptyBorder(10, 20, 10, 20);
        button.setBorder(new CompoundBorder(lineBorder, emptyBorder));

        button.putClientProperty("Nimbus.Overrides", true);
        button.putClientProperty("Nimbus.Overrides.InheritDefaults", false);
        button.setForeground(TEXT_COLOR);
        button.setOpaque(true);

        return button;
    }

    private JTextArea createStyledTextArea() {
        JTextArea textArea = new JTextArea();
        textArea.setFont(DEFAULT_FONT);
        textArea.setBackground(Color.WHITE);
        textArea.setForeground(TEXT_COLOR);
        textArea.setWrapStyleWord(true);
        textArea.setLineWrap(true);
        return textArea;
    }

    private JTextArea createStyledTextArea(Font font) {
        JTextArea textArea = new JTextArea();
        textArea.setFont(font);
        textArea.setBackground(Color.WHITE);
        textArea.setForeground(TEXT_COLOR);
        textArea.setWrapStyleWord(true);
        textArea.setLineWrap(true);
        return textArea;
    }

    private JTextField createStyledTextField() {
        JTextField textField = new JTextField();
        textField.setFont(DEFAULT_FONT);
        textField.setBackground(Color.WHITE);
        textField.setForeground(TEXT_COLOR);
        Border lineBorder = new LineBorder(new Color(200, 200, 200), 1);
        Border emptyBorder = new EmptyBorder(10, 12, 10, 12);
        textField.setBorder(new CompoundBorder(lineBorder, emptyBorder));
        return textField;
    }

    private JScrollPane createStyledScrollPane(JComponent component, String title) {
        JScrollPane scrollPane = new JScrollPane(component);
        scrollPane.setBorder(BorderFactory.createTitledBorder(new LineBorder(PRIMARY_COLOR, 2), title,
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                DEFAULT_FONT, PRIMARY_COLOR));
        return scrollPane;
    }

    private void generatePdfFromText(String text, File outputFile) throws IOException {
        PDDocument document = new PDDocument();
        PDPage page = new PDPage();
        document.addPage(page);
        PDPageContentStream contentStream = null;
        try {
            contentStream = new PDPageContentStream(document, page);
            contentStream.setFont(PDType1Font.COURIER, 12);
            float leading = 14.5f;
            contentStream.setLeading(leading);

            float margin = 50;float yPosition = page.getMediaBox().getHeight() - margin;
            float startX = margin;

            String[] lines = text.split("\n");

            for (String line : lines)
            {
                List<String> wrappedLines = new ArrayList<>();
                while (line.length() > 0) {
                    int breakIndex = Math.min(line.length(), (int) ((page.getMediaBox().getWidth() - 2 * margin) / (PDType1Font.COURIER.getStringWidth(line.substring(0, 1)) / 1000 * 12)));
                    wrappedLines.add(line.substring(0, breakIndex));
                    line = line.substring(breakIndex).trim();
                }

                for (String wrappedLine : wrappedLines) {
                    try {
                        contentStream.beginText();
                        contentStream.newLineAtOffset(startX, yPosition);
                        contentStream.showText(wrappedLine);
                        contentStream.endText();
                        yPosition -= leading;

                        if (yPosition < margin) {
                            contentStream.close();
                            page = new PDPage();
                            document.addPage(page);
                            contentStream = new PDPageContentStream(document, page);
                            contentStream.setFont(PDType1Font.COURIER, 12);
                            contentStream.setLeading(leading);
                            yPosition = page.getMediaBox().getHeight() - margin;
                        }
                    } catch (IOException e) {
                        System.err.println("Error writing line to PDF: " + wrappedLine + " - " + e.getMessage());
                    }
                }
            }

        } finally {
            if (contentStream != null) {
                try {
                    contentStream.close();
                } catch (IOException e) {
                    System.err.println("Error closing content stream: " + e.getMessage());
                }
            }
            document.save(outputFile);
            document.close();
        }
    }

    private void generateAIAnalysis(String resumeText) {
        try {
            String prompt = "Analyze the following resume for ATS compatibility, format, sections, skills, style. " +
                    "Identify key skills, provide a summary of ATS keyword matches.\n\n" +
                    "RESUME:\n" + resumeText.substring(0, Math.min(4000, resumeText.length())) +
                    "\n\nProvide the analysis in the following format:\n" +
                    "ATS COMPATIBILITY SCORE: [Numerical score out of 100]\n" +
                    "ATS COMPATIBILITY DETAILS: [Summary based on keywords and formatting]\n" +
                    "FORMAT: [Strengths and weaknesses of the formatting]\n" +
                    "SECTIONS: [Completeness and relevance of the sections]\n" +
                    "SKILLS: [Key skills identified]\n" +
                    "STYLE: [Professionalism and clarity of writing style]\n" +
                    "ATS Keyword Matches: [List of key categories and whether relevant keywords are present]";

            String aiResponse = callCohereAPI(prompt);
            resumeResultText.setText(aiResponse);

            Pattern pattern = Pattern.compile("ATS COMPATIBILITY SCORE: (\\d+)");
            Matcher matcher = pattern.matcher(aiResponse);
            int atsScore = 0;
            if (matcher.find()) {
                try {
                    atsScore = Integer.parseInt(matcher.group(1));
                } catch (NumberFormatException e) {
                    System.err.println("Could not parse ATS score: " + matcher.group(1));
                }
            }
            dualScorePanel.setAtsScore(atsScore);

            // Extract and display detailed analysis
            extractAndDisplayGrammarAndLanguage(aiResponse, resumeText); // Pass resumeText

        } catch (Exception e) {
            resumeResultText.setText("Error during AI analysis: " + e.getMessage());
            dualScorePanel.setAtsScore(0);
            dualScorePanel.setInterviewProbability(0);
            e.printStackTrace();
        }
    }

    private void extractAndDisplayGrammarAndLanguage(String aiResponse, String resumeText) {
        //  Prompt specifically for grammar/language after the main analysis
        String grammarPrompt = "Analyze the following resume for grammar and language quality. " +
                "Identify specific errors and suggest improvements.\n\n" +
                "RESUME:\n" + resumeText.substring(0, Math.min(4000, resumeText.length()));

        try {
            String grammarResponse = callCohereAPI(grammarPrompt);
            if (grammarResponse != null && !grammarResponse.trim().isEmpty()) {
                grammarAndLanguageQualityText.setText(grammarResponse.trim());
            } else {
                grammarAndLanguageQualityText.setText("No specific feedback on grammar and language quality.");
            }
        } catch (Exception e) {
            grammarAndLanguageQualityText.setText("Error retrieving grammar analysis: " + e.getMessage());
            e.printStackTrace(); // Log the error
        }
    }

    private String extractSection(String text, String sectionHeader) {
        // Make the search case-insensitive and allow for some variations in spacing
        String regexHeader = "(?i)\\s*" + Pattern.quote(sectionHeader) + "\\s*:?\\s*\\n?";
        Pattern pattern = Pattern.compile(regexHeader);
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            int start = matcher.end(); // Start after the header
            int nextSectionStart = -1;

            // Find the start of the next major section (e.g., "ATS COMPATIBILITY SCORE:")
            String[] majorHeaders = {"ATS COMPATIBILITY SCORE:", "FORMAT:", "SECTIONS:", "SKILLS:", "STYLE:", "ATS Keyword Matches:"}; // Add more headers as needed
            for (String header : majorHeaders) {
                String regexNextHeader = "(?i)\\s*" + Pattern.quote(header) + "\\s*:?\\s*\\n?";
                Pattern nextPattern = Pattern.compile(regexNextHeader);
                Matcher nextMatcher = nextPattern.matcher(text.substring(start));
                if (nextMatcher.find()) {
                    nextSectionStart = start + nextMatcher.start();
                    break;
                }
            }

            if (nextSectionStart != -1) {
                return text.substring(start, nextSectionStart).trim();
            } else {
                return text.substring(start).trim(); // Extract to the end of the text
            }
        }
        return null;
    }
    private void performResumeComparison() {
        resumeResultText.setText("Analyzing and comparing " + uploadedResumes.size() + " resumes...\n");
        analysisResults.clear();
        atsScores.clear();
        rankedResumes.clear();
        grammarAndLanguageQualityText.setText(""); // Clear previous grammar results

        for (File resumeFile : uploadedResumes) {
            String resumeText = resumeTexts.get(resumeFile);
            try {
                String analysis = generateAIAnalysisForComparison(resumeText);
                analysisResults.put(resumeFile, analysis);
                int score = extractATSScore(analysis);
                atsScores.put(resumeFile, score);

                resumeResultText.append("--- Analysis for: " + resumeFile.getName() + " ---\n");
                resumeResultText.append("ATS Score: " + score + "/100\n");
                resumeResultText.append(analysis + "\n\n");

                //  Perform and display grammar analysis
                String grammarAnalysis = generateGrammarAnalysis(resumeText);
                grammarAndLanguageQualityText.append("--- Grammar Analysis for: " + resumeFile.getName() + " ---\n");
                grammarAndLanguageQualityText.append(grammarAnalysis + "\n\n");

            } catch (Exception e) {
                resumeResultText.append("Error during AI analysis for: " + resumeFile.getName() + " - " + e.getMessage() + "\n");
                grammarAndLanguageQualityText.append("Error during grammar analysis for: " + resumeFile.getName() + " - " + e.getMessage() + "\n");
                e.printStackTrace();
            }
        }

        rankedResumes = new ArrayList<>(atsScores.entrySet());
        rankedResumes.sort(Map.Entry.<File, Integer>comparingByValue().reversed());

        resumeResultText.append("\n--- Resume Ranking (Based on ATS Score) ---\n");
        for (int i = 0; i < rankedResumes.size(); i++) {
            Entry<File, Integer> entry = rankedResumes.get(i);
            resumeResultText.append("Rank " + (i + 1) + ": " + entry.getKey().getName() + " - ATS Score: " + entry.getValue() + "/100\n");
        }
    }

    private String generateGrammarAnalysis(String resumeText) throws Exception {
        String grammarPrompt = "Analyze the following resume for grammar and language quality. " +
                "Identify specific errors and suggest improvements.\n\n" +
                "RESUME:\n" + resumeText.substring(0, Math.min(4000, resumeText.length()));

        return callCohereAPI(grammarPrompt);
    }

    private String extractTextFromPDF(File file) throws IOException {
        try (PDDocument document = PDDocument.load(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String generateAIAnalysisForComparison(String resumeText) throws Exception {
        String prompt = "Analyze the following resume for ATS compatibility, format, sections, skills, and style. " +
                "Identify key skills and provide a summary of ATS keyword matches. " +
                "Based on your analysis, provide an ATS compatibility score out of 100.\n\n" +
                "RESUME:\n" + resumeText.substring(0, Math.min(4000, resumeText.length())) +
                "\n\nProvide the analysis in the following format:\n" +
                "ATS COMPATIBILITY SCORE: [Numerical score out of 100]\n" +
                "ATS COMPATIBILITY DETAILS: [Summary based on keywords and formatting]\n" +
                "FORMAT: [Strengths and weaknesses of the formatting]\n" +
                "SECTIONS: [Completeness and relevance of the sections]\n" +
                "SKILLS: [Key skills identified]\n" +
                "STYLE: [Professionalism and clarity of writing style]\n" +
                "ATS Keyword Matches: [List of key categories and whether relevant keywords are present]";

        return callCohereAPI(prompt);
    }


    private int extractATSScore(String analysisResult) {
        String scoreLine = null;
        Scanner scanner = new Scanner(analysisResult);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.startsWith("ATS COMPATIBILITY SCORE:")) {
                scoreLine = line.substring("ATS COMPATIBILITY SCORE:".length()).trim();
                break;
            }
        }
        scanner.close();

        try {
            if (scoreLine != null) {
                return Integer.parseInt(scoreLine.replaceAll("[^0-9]", ""));
            }
        } catch (NumberFormatException e) {
            System.err.println("Could not parse ATS score: " + scoreLine);
        }
        return 0;
    }

    private String callCohereAPI(String prompt) throws Exception {
        URL url = new URL(COHERE_API_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", "Bearer " + COHERE_API_KEY);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        JSONObject requestBody = new JSONObject();
        requestBody.put("prompt", prompt);
        requestBody.put("max_tokens", 800);
        requestBody.put("temperature", 0.5);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line.trim());
            }
        } finally {
            connection.disconnect();
        }

        JSONObject jsonResponse = new JSONObject(response.toString());
        JSONArray generations = jsonResponse.getJSONArray("generations");
        if (generations.length() > 0) {
            return generations.getJSONObject(0).getString("text").trim();
        }
        return "No response from AI.";
    }

    private static List<String> callAI(String task, String userQuery) {
        try {
            URL apiUrl = new URL(COHERE_API_URL);
            HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer " + COHERE_API_KEY);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            JSONObject body = new JSONObject();
            body.put("model", "command");
            body.put("prompt", task + "\n\n" + userQuery);
            body.put("max_tokens", 350);

            try (OutputStream os = connection.getOutputStream()) {
                os.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                System.out.println("Error: API responded with code " + responseCode);
                return Arrays.asList("Error: Unable to process request.");
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            br.close();

            return extractResponseText(response.toString());
        } catch (Exception e) {
            System.out.println("API Error: " + e.getMessage());
            return Arrays.asList("Error: Unable to connect to AI service.");
        }
    }

    private static List<String> extractResponseText(String jsonResponse) {
        try {
            JSONObject responseObject = new JSONObject(jsonResponse);
            if (responseObject.has("generations")) {
                JSONArray generations = responseObject.getJSONArray("generations");
                List<String> responses = new ArrayList<>();
                for (int i = 0; i < generations.length(); i++) {
                    JSONObject gen = generations.getJSONObject(i);
                    if (gen.has("text")) {
                        responses.add(gen.getString("text").trim());
                    }
                }
                return responses;
            }
        } catch (Exception e) {
            System.out.println("JSON Parsing Error: " + e.getMessage());
        }
        return Arrays.asList("No valid response from AI.");
    }
}

class DualScorePanel extends JPanel {
    private int atsScore = 0;
    private int interviewProbability = 0;
    private final Color trackColor = new Color(220, 220, 220);
    private final Color atsProgressColor = new Color(76, 175, 80); // Green for ATS
    private final Color interviewProgressColor = new Color(255, 165, 0); // Orange for Interview Probability
    private final Color textColor = new Color(51, 51, 51);
    private final Font scoreFont = new Font("Arial", Font.BOLD, 20);
    private final Font labelFont = new Font("Arial", Font.PLAIN, 12);

    public void setAtsScore(int newScore) {
        this.atsScore = Math.max(0, Math.min(100, newScore));
        repaint();
    }

    public void setInterviewProbability(int newProbability) {
        this.interviewProbability = Math.max(0, Math.min(100, newProbability));
        repaint();
    }

    public int getAtsScore() {
        return atsScore;
    }

    public int getInterviewProbability() {
        return interviewProbability;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        int diameter = Math.min(width, height) - 20; // Adjust diameter for spacing
        int x = (width - diameter) / 2;
        int y = 10;

        // --- ATS Score Speedometer ---
        g2d.setColor(trackColor);
        g2d.setStroke(new BasicStroke(8));
        g2d.draw(new Arc2D.Double(x, y, diameter / 2, diameter / 2, 0, 180, Arc2D.OPEN));

        double atsExtent = (atsScore / 100.0) * 180;
        g2d.setColor(atsProgressColor);
        g2d.setStroke(new BasicStroke(8));
        g2d.draw(new Arc2D.Double(x, y, diameter / 2, diameter / 2, 0, atsExtent, Arc2D.OPEN));

        g2d.setFont(scoreFont);
        g2d.setColor(textColor);
        String atsScoreText = atsScore + "%";
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(atsScoreText);
        g2d.drawString(atsScoreText, x + (diameter / 4 - textWidth) / 2, y + diameter / 2 + fm.getAscent());

        g2d.setFont(labelFont);
        String atsLabel = "ATS Score";
        int labelWidth = fm.stringWidth(atsLabel);
        g2d.drawString(atsLabel, x + (diameter / 4 - labelWidth) / 2, y + diameter / 2 + fm.getAscent() + 15);

        // --- Interview Probability Speedometer ---
        int interviewX = x + diameter / 2 + 20; // Increased spacing here
        g2d.setColor(trackColor);
        g2d.setStroke(new BasicStroke(8));
        g2d.draw(new Arc2D.Double(interviewX, y, diameter / 2, diameter / 2, 0, 180, Arc2D.OPEN));

        double interviewExtent = (interviewProbability / 100.0) * 180;
        g2d.setColor(interviewProgressColor);
        g2d.setStroke(new BasicStroke(8));
        g2d.draw(new Arc2D.Double(interviewX, y, diameter / 2, diameter / 2, 0, interviewExtent, Arc2D.OPEN));

        g2d.setFont(scoreFont);
        g2d.setColor(textColor);
        String interviewProbText = interviewProbability + "%";
        int interviewTextWidth = fm.stringWidth(interviewProbText);
        g2d.drawString(interviewProbText, interviewX + (diameter / 4 - interviewTextWidth) / 2, y + diameter / 2 + fm.getAscent());

        g2d.setFont(labelFont);
        String interviewLabel = "Interview Prob.";
        int interviewLabelWidth = fm.stringWidth(interviewLabel);
        g2d.drawString(interviewLabel, interviewX + (diameter / 4 - interviewLabelWidth) / 2, y + diameter / 2 + fm.getAscent() + 15);


        g2d.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(220, 200); // Increased width to accommodate spacing
    }
}
