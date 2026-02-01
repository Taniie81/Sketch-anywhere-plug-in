import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class DrawingDialog extends DialogWrapper {
    private DrawingCanvas canvas;
    // On garde la clé ici pour l'utiliser dans les deux méthodes
    private final String apiKey =  "\t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"; // <--- Mets ta clé sk-... ici;

    public DrawingDialog() {
        super(true);
        setTitle("Dessin & Design AI Assistant");
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        canvas = new DrawingCanvas();
        return canvas;
    }

    @Override
    protected void doOKAction() {
        String base64Image = canvas.getBase64Image();

        new Task.Backgroundable(null, "L'IA génère votre SVG...", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    // 1. Appel à GPT pour le SVG
                    String responseJson = callOpenAiApi(base64Image);

                    // Extraction et nettoyage du JSON
                    String rawContent = responseJson.split("\"content\": \"")[1].split("\"")[0];
                    String cleanContent = rawContent.replace("\\n", "\n")
                            .replace("\\t", "    ")
                            .replace("\\\"", "\"");

                    // Extraction du nom (ex: "mountain")
                    String itemName = "icon";
                    if (cleanContent.contains("You drew a ")) {
                        itemName = cleanContent.split("You drew a ")[1].split("\\.")[0].trim();
                    }

                    // Extraction du bloc SVG
                    String svgCode = "";
                    if (cleanContent.contains("<svg") && cleanContent.contains("</svg>")) {
                        svgCode = cleanContent.substring(cleanContent.indexOf("<svg"), cleanContent.lastIndexOf("</svg>") + 6);
                    } else {
                        svgCode = cleanContent;
                    }

                    // 2. Génération de l'image DALL-E 3
                    String imageUrl = generateImageWithDalle(itemName);

                    // --- CORRECTION DU ROUGE ICI ---
                    // On crée des copies finales pour le thread UI
                    final String finalItemName = itemName;
                    final String finalImageUrl = imageUrl;
                    final String finalSvg = svgCode;

                    SwingUtilities.invokeLater(() -> {
                        try {
                            // COPIE AUTO
                            java.awt.datatransfer.StringSelection selection = new java.awt.datatransfer.StringSelection(finalSvg);
                            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);

                            // INTERFACE
                            JTextArea textArea = new JTextArea(finalSvg);
                            textArea.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 12));
                            textArea.setEditable(false);
                            JScrollPane scrollPane = new JScrollPane(textArea);
                            scrollPane.setPreferredSize(new java.awt.Dimension(600, 300));

                            JPanel panel = new JPanel(new java.awt.BorderLayout(10, 10));
                            JPanel northPanel = new JPanel(new java.awt.GridLayout(3, 1, 5, 5));

                            northPanel.add(new JLabel("✨ SVG Code copied to clipboard (Object: " + finalItemName + ")"));

                            JButton downloadBtn = new JButton("Download HD Image (DALL-E 3) 📥");
                            downloadBtn.addActionListener(e -> {
                                try { java.awt.Desktop.getDesktop().browse(new URI(finalImageUrl)); } catch (Exception ex) {}
                            });
                            northPanel.add(downloadBtn);

                            JButton previewBtn = new JButton("Preview SVG in Browser 🌐");
                            previewBtn.addActionListener(e -> {
                                try {
                                    java.io.File tempFile = java.io.File.createTempFile("preview", ".svg");
                                    java.nio.file.Files.writeString(tempFile.toPath(), finalSvg);
                                    java.awt.Desktop.getDesktop().browse(tempFile.toURI());
                                } catch (Exception ex) {}
                            });
                            northPanel.add(previewBtn);

                            panel.add(northPanel, java.awt.BorderLayout.NORTH);
                            panel.add(scrollPane, java.awt.BorderLayout.CENTER);

                            JOptionPane.showMessageDialog(null, panel, "Résultat IA", JOptionPane.PLAIN_MESSAGE);

                        } catch (Exception e) {
                            Messages.showErrorDialog("Erreur d'affichage : " + e.getMessage(), "Oups");
                        }
                        close(OK_EXIT_CODE);
                    });
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> Messages.showErrorDialog("Erreur : " + e.getMessage(), "Échec"));
                }
            }
        }.queue();
    }

    private String callOpenAiApi(String base64Data) throws Exception {
        String url = "https://api.openai.com/v1/chat/completions";

        // On s'assure que le base64 est sur une seule ligne propre
        String cleanBase64 = base64Data.replaceAll("\\s", "");

        // Construction du JSON avec String.format pour éviter les erreurs de "+"
        String jsonBody = "{"
                + "\"model\": \"gpt-4o-mini\","
                + "\"messages\": ["
                + "  {"
                + "    \"role\": \"user\","
                + "    \"content\": ["
                + "      {\"type\": \"text\", \"text\": \"Identify this drawing. Start with 'You drew a [name]'. Then, provide the SVG code. IMPORTANT: Use an ultra-simple 2D flat vector style. Only black color to be used, No shadows, no gradients, just clean minimalist shapes and full white background, it should be the same as the svg drawing. Do not use markdown code blocks. Start the SVG directly with <svg and end with </svg>. Use single quotes for SVG attributes instead of double quotes.\"},"
                + "      {\"type\": \"image_url\", \"image_url\": {\"url\": \"data:image/png;base64," + cleanBase64 + "\"}}"
                + "    ]"
                + "  }"
                + "]"
                + "}";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey.trim())
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            // Cela affichera l'erreur précise dans ta console d'exécution
            System.err.println("Détail de l'erreur : " + response.body());
            throw new Exception("Erreur API : " + response.statusCode());
        }

        return response.body();
    }

    private String generateImageWithDalle(String description) throws Exception {
        String url = "https://api.openai.com/v1/images/generations";
        String jsonBody = "{"
                + "\"model\": \"dall-e-3\","
                + "\"prompt\": \"A simple icon of a " + description + ",really simple and easy, for a icone application,in 2D and in a whole white background,like a blank page\","
                + "\"n\": 1,"
                + "\"size\": \"1024x1024\""
                + "}";

        String response = sendHttpRequest(url, jsonBody);
        // Extraction de l'URL
        return response.split("\"url\": \"")[1].split("\"")[0];
    }

    private String sendHttpRequest(String url, String jsonBody) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey.trim())
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) throw new Exception("Erreur " + response.statusCode());
        return response.body();
    }
}