import javax.swing.*;
import javax.sound.sampled.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage; // Add this import for BufferedImage
import java.io.*;
import java.util.*;
import java.util.List;


public class CandyMemoryGame extends JFrame {
    private static final int GRID_SIZE = 4; // 4x4 grid
    private static final int GAME_DURATION = 40; // 40 seconds countdown
    private List<Card> cards;
    private Card firstSelectedCard;
    private Card secondSelectedCard;
    private int pairsFound;
    private javax.swing.Timer gameTimer;
    private int timeRemaining;
    
    // Sound effects
    private Clip flipSound;
    private Clip matchSound;
    private Clip winSound;
    private Clip loseSound;
    
    // UI Components
    private JPanel gamePanel;
    private JLabel timerLabel;
    private JLabel pairsLabel;
    private JButton newGameButton;
    private JLabel resultLabel;
    
    // Color theme
    private static final Color BACKGROUND_COLOR = new Color(235, 222, 240); // Light purple
    private static final Color ACCENT_COLOR = new Color(160, 100, 200); // Medium purple
    private static final Color TITLE_COLOR = new Color(90, 50, 120); // Dark purple
    private static final Color CARD_COLOR = new Color(210, 190, 230); // Very light purple
    
    // Candy images
    private Image[] candyImages;

    public CandyMemoryGame() {
        super("Candy Memory Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(700, 800);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);
        
        // Load sounds
        loadSounds();
        
        // Load candy images
        loadCandyImages();
        
        initializeGame();
        setupUI();
        
        setVisible(true);
    }
    
    private void loadCandyImages() {
        try {
            // Create array to hold 8 different candy images
            candyImages = new Image[8];
            
            // Load embedded candy images or create placeholder images
            candyImages[0] = createCandyImage(new Color(255, 100, 100)); // Red candy
            candyImages[1] = createCandyImage(new Color(100, 200, 100)); // Green candy
            candyImages[2] = createCandyImage(new Color(100, 100, 255)); // Blue candy
            candyImages[3] = createCandyImage(new Color(255, 200, 0));   // Yellow candy
            candyImages[4] = createCandyImage(new Color(200, 100, 200)); // Purple candy
            candyImages[5] = createCandyImage(new Color(255, 150, 0));   // Orange candy
            candyImages[6] = createCandyImage(new Color(100, 200, 200)); // Cyan candy
            candyImages[7] = createCandyImage(new Color(255, 180, 180)); // Pink candy
            
        } catch (Exception e) {
            System.out.println("Error loading candy images: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Create a candy icon image since we don't have actual image files
    private Image createCandyImage(Color baseColor) {
        int size = 80;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        
        // Enable anti-aliasing
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw candy shape (a circle with decorations)
        g.setColor(baseColor);
        g.fillOval(5, 5, size - 10, size - 10);
        
        // Add highlights
        g.setColor(new Color(255, 255, 255, 100));
        g.fillOval(15, 10, 25, 20);
        
        // Add stripes
        g.setColor(baseColor.darker());
        for (int i = 0; i < 3; i++) {
            g.drawLine(25, 15 + i*15, 55, 15 + i*15);
        }
        
        // Add outline
        g.setColor(baseColor.darker().darker());
        g.setStroke(new BasicStroke(2));
        g.drawOval(5, 5, size - 10, size - 10);
        
        g.dispose();
        return image;
    }
    
    private void loadSounds() {
        try {
            // Create embedded sounds
            createEmbeddedSounds();
        } catch (Exception e) {
            System.out.println("Error loading sounds: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // The rest of the createEmbeddedSounds(), createBeepSound(), and createSequenceSound() methods remain unchanged
    // Create embedded sounds when sound files aren't available
    private void createEmbeddedSounds() {
        try {
            // Create basic sounds programmatically
            flipSound = createBeepSound(300, 50);  // Short high tone
            matchSound = createBeepSound(600, 150); // Medium high tone
            winSound = createSequenceSound(new int[]{600, 800, 1000}, new int[]{150, 150, 300}); // Rising tones
            loseSound = createSequenceSound(new int[]{400, 300, 200}, new int[]{150, 150, 300}); // Falling tones
        } catch (Exception e) {
            System.out.println("Failed to create embedded sounds: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Create a simple beep sound with specified frequency and duration
    private Clip createBeepSound(int frequency, int duration) {
        try {
            AudioFormat format = new AudioFormat(44100, 8, 1, true, false);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            
            // Generate a simple sine wave
            float sampleRate = format.getSampleRate();
            byte[] buffer = new byte[(int)(duration * format.getSampleRate() / 1000)];
            for (int i = 0; i < buffer.length; i++) {
                double angle = 2.0 * Math.PI * i / (sampleRate / frequency);
                buffer[i] = (byte)(Math.sin(angle) * 127.0);
            }
            baos.write(buffer);
            
            byte[] audioData = baos.toByteArray();
            AudioInputStream ais = new AudioInputStream(
                new ByteArrayInputStream(audioData),
                format,
                audioData.length / format.getFrameSize()
            );
            
            Clip clip = AudioSystem.getClip();
            clip.open(ais);
            return clip;
        } catch (Exception e) {
            System.out.println("Error creating beep sound: " + e.getMessage());
            return null;
        }
    }
    
    // Create a sequence of tones
    private Clip createSequenceSound(int[] frequencies, int[] durations) {
        try {
            AudioFormat format = new AudioFormat(44100, 8, 1, true, false);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            float sampleRate = format.getSampleRate();
            
            for (int t = 0; t < frequencies.length; t++) {
                int frequency = frequencies[t];
                int duration = durations[t];
                
                byte[] buffer = new byte[(int)(duration * format.getSampleRate() / 1000)];
                for (int i = 0; i < buffer.length; i++) {
                    double angle = 2.0 * Math.PI * i / (sampleRate / frequency);
                    buffer[i] = (byte)(Math.sin(angle) * 127.0);
                }
                baos.write(buffer);
            }
            
            byte[] audioData = baos.toByteArray();
            AudioInputStream ais = new AudioInputStream(
                new ByteArrayInputStream(audioData),
                format,
                audioData.length / format.getFrameSize()
            );
            
            Clip clip = AudioSystem.getClip();
            clip.open(ais);
            return clip;
        } catch (Exception e) {
            System.out.println("Error creating sequence sound: " + e.getMessage());
            return null;
        }
    }
    
    private void playSound(Clip sound) {
        try {
            if (sound != null) {
                // Stop the sound if it's currently playing
                if (sound.isRunning()) {
                    sound.stop();
                }
                sound.setFramePosition(0);
                sound.start();
            }
        } catch (Exception e) {
            System.out.println("Error playing sound: " + e.getMessage());
        }
    }

    private void initializeGame() {
        cards = new ArrayList<>();
        firstSelectedCard = null;
        secondSelectedCard = null;
        pairsFound = 0;
        timeRemaining = GAME_DURATION;
        gameTimer = new javax.swing.Timer(1000, e -> {
            timeRemaining--;
            updateTimerLabel();
            
            if (timeRemaining <= 0) {
                gameTimer.stop();
                checkGameEnd();
            }
        });
    }
    
    // The setupUI() method remains unchanged
    private void setupUI() {
        // Candy background
        setContentPane(new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                
                GradientPaint gradient = new GradientPaint(
                    0, 0, BACKGROUND_COLOR, 
                    getWidth(), getHeight(), new Color(BACKGROUND_COLOR.getRed() - 20, 
                                                     BACKGROUND_COLOR.getGreen() - 20, 
                                                     BACKGROUND_COLOR.getBlue() - 20));
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                
                // Add some candy-like decorations
                g2d.setColor(new Color(ACCENT_COLOR.getRed(), ACCENT_COLOR.getGreen(), 
                                     ACCENT_COLOR.getBlue(), 40));
                
                // Draw some decorative circles
                for (int i = 0; i < 20; i++) {
                    int size = 20 + (int)(Math.random() * 60);
                    int x = (int)(Math.random() * getWidth());
                    int y = (int)(Math.random() * getHeight());
                    g2d.fillOval(x, y, size, size);
                }
            }
        });
        
        // Header Panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        
        // Game Title
        JLabel titleLabel = new JLabel("CANDY MEMORY GAME", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        titleLabel.setForeground(TITLE_COLOR);
        headerPanel.add(titleLabel, BorderLayout.NORTH);
        
        // Stats Panel
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 30,
        10));
        statsPanel.setOpaque(false);
        
        timerLabel = new JLabel("⏱️ 0:40");
        timerLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        timerLabel.setForeground(TITLE_COLOR);
        statsPanel.add(timerLabel);
        
        pairsLabel = new JLabel("🍬 Pairs: 0/8");
        pairsLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        pairsLabel.setForeground(TITLE_COLOR);
        statsPanel.add(pairsLabel);
        
        resultLabel = new JLabel("");
        resultLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        statsPanel.add(resultLabel);
        
        headerPanel.add(statsPanel, BorderLayout.CENTER);
        
        // New Game Button
        newGameButton = createStyledButton("NEW GAME");
        newGameButton.addActionListener(e -> startNewGame());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setOpaque(false);
        buttonPanel.add(newGameButton);
        headerPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        add(headerPanel, BorderLayout.NORTH);
        
        // Game Panel
        gamePanel = new JPanel(new GridLayout(GRID_SIZE, GRID_SIZE, 10, 10));
        gamePanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        gamePanel.setOpaque(false);
        
        // Wrap game panel in another panel for better styling
        JPanel gamePanelWrapper = new JPanel(new BorderLayout());
        gamePanelWrapper.setOpaque(false);
        gamePanelWrapper.setBorder(BorderFactory.createEmptyBorder(0, 20, 20, 20));
        gamePanelWrapper.add(gamePanel, BorderLayout.CENTER);
        
        add(gamePanelWrapper, BorderLayout.CENTER);
        
        startNewGame();
    }
    
    private JButton createStyledButton(String text) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Create rounded button
                RoundRectangle2D roundedRectangle = new RoundRectangle2D.Float(
                    0, 0, getWidth() - 1, getHeight() - 1, 15, 15);
                
                // Set gradient paint
                GradientPaint gp = new GradientPaint(
                    0, 0, ACCENT_COLOR,
                    0, getHeight(), new Color(ACCENT_COLOR.getRed() - 30, 
                                           ACCENT_COLOR.getGreen() - 30, 
                                           ACCENT_COLOR.getBlue() - 30));
                
                g2d.setPaint(gp);
                g2d.fill(roundedRectangle);
                
                // Draw gloss effect
                if (getModel().isPressed()) {
                    g2d.setColor(new Color(0, 0, 0, 50));
                    g2d.fill(roundedRectangle);
                } else if (getModel().isRollover()) {
                    g2d.setColor(new Color(255, 255, 255, 30));
                    g2d.fill(roundedRectangle);
                }
                
                // Draw the text
                FontMetrics fm = g2d.getFontMetrics();
                Rectangle2D r = fm.getStringBounds(getText(), g2d);
                int x = (getWidth() - (int) r.getWidth()) / 2;
                int y = (getHeight() - (int) r.getHeight()) / 2 + fm.getAscent();
                
                g2d.setFont(getFont());
                g2d.setColor(Color.WHITE);
                g2d.drawString(getText(), x, y);
            }
        };
        
        button.setFont(new Font("Segoe UI", Font.BOLD, 16));
        button.setForeground(Color.WHITE);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setPreferredSize(new Dimension(150, 40));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        return button;
    }
    
    private void startNewGame() {
        gameTimer.stop();
        initializeGame();
        cards.clear();
        gamePanel.removeAll();
        
        // Create card pairs
        createCardPairs();
        Collections.shuffle(cards);
        
        // Assign random numbers to cards (1-16)
        List<Integer> cardNumbers = new ArrayList<>();
        for (int i = 1; i <= GRID_SIZE * GRID_SIZE; i++) {
            cardNumbers.add(i);
        }
        Collections.shuffle(cardNumbers);
        
        // Create card buttons
        for (int i = 0; i < cards.size(); i++) {
            Card card = cards.get(i);
            int cardNumber = cardNumbers.get(i);
            card.setNumber(cardNumber);
            
            JButton button = createCardButton(card);
            card.setButton(button);
            gamePanel.add(button);
        }
        
        updateStatsLabels();
        resultLabel.setText("");
        resultLabel.setForeground(Color.BLACK);
        timeRemaining = GAME_DURATION;
        updateTimerLabel();
        gameTimer.start();
        
        gamePanel.revalidate();
        gamePanel.repaint();
    }
    
    private JButton createCardButton(Card card) {
        JButton button = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Create rounded card
                RoundRectangle2D roundedRectangle = new RoundRectangle2D.Float(
                    0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
                
                if (card.isRevealed()) {
                    // Card front - show candy image
                    g2d.setColor(Color.WHITE);
                    g2d.fill(roundedRectangle);
                    
                    // Draw candy image
                    Image candyImage = card.getCandyImage();
                    if (candyImage != null) {
                        int margin = 10;
                        g2d.drawImage(candyImage, margin, margin, 
                                     getWidth() - 2 * margin, getHeight() - 2 * margin, this);
                    }
                    
                    // Outer border
                    g2d.setColor(ACCENT_COLOR);
                    g2d.setStroke(new BasicStroke(3));
                    g2d.draw(roundedRectangle);
                } else {
                    // Card back - show number
                    g2d.setColor(CARD_COLOR);
                    g2d.fill(roundedRectangle);
                    
                    // Add some candy-like pattern
                    g2d.setColor(new Color(ACCENT_COLOR.getRed(), ACCENT_COLOR.getGreen(), 
                                         ACCENT_COLOR.getBlue(), 40));
                    for (int i = 0; i < 5; i++) {
                        int size = 10 + (int)(Math.random() * 15);
                        int x = (int)(Math.random() * getWidth());
                        int y = (int)(Math.random() * getHeight());
                        g2d.fillOval(x, y, size, size);
                    }
                    
                    // Border
                    g2d.setColor(ACCENT_COLOR);
                    g2d.setStroke(new BasicStroke(3));
                    g2d.draw(roundedRectangle);
                    
                    // Draw the card number
                    Font numberFont = new Font("Segoe UI", Font.BOLD, 26);
                    g2d.setFont(numberFont);
                    FontMetrics fm = g2d.getFontMetrics();
                    String numberText = String.valueOf(card.getNumber());
                    Rectangle2D r = fm.getStringBounds(numberText, g2d);
                    int x = (getWidth() - (int) r.getWidth()) / 2;
                    int y = (getHeight() - (int) r.getHeight()) / 2 + fm.getAscent();
                    
                    g2d.setColor(TITLE_COLOR);
                    g2d.drawString(numberText, x, y);
                }
            }
        };
        
        button.setPreferredSize(new Dimension(100, 100));
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.addActionListener(e -> handleCardClick(card, button));
        
        return button;
    }
    
    private void createCardPairs() {
        // We'll create 8 pairs (16 cards) with 8 different candy images
        for (int i = 0; i < 8; i++) {
            Image candyImage = candyImages[i];
            cards.add(new Card(candyImage, i));
            cards.add(new Card(candyImage, i));
        }
    }
    
    // The rest of the methods remain mostly unchanged
    private void handleCardClick(Card card, JButton button) {
        if (card.isMatched() || card.isRevealed() || secondSelectedCard != null || timeRemaining <= 0) {
            return;
        }
        
        // Play flip sound when card is clicked
        playSound(flipSound);
        
        card.setRevealed(true);
        button.repaint(); // Repaint to show the candy image
        
        if (firstSelectedCard == null) {
            firstSelectedCard = card;
        } else {
            secondSelectedCard = card;
            checkForMatch();
        }
        
        updateStatsLabels();
    }
    
    private void checkForMatch() {
        // Compare the candy image indices of the two selected cards
        boolean isMatch = firstSelectedCard.getCandyImageIndex() == secondSelectedCard.getCandyImageIndex();
        
        if (isMatch) {
            // Play match sound
            playSound(matchSound);
            
            firstSelectedCard.setMatched(true);
            secondSelectedCard.setMatched(true);
            pairsFound++;
            if (pairsFound >= 8) { // Win condition: 8 pairs matched
                gameTimer.stop();
                showResult(true);
            }
            
            firstSelectedCard = null;
            secondSelectedCard = null;
        } else {
            javax.swing.Timer flipBackTimer = new javax.swing.Timer(1000, e -> {
                firstSelectedCard.setRevealed(false);
                secondSelectedCard.setRevealed(false);
                firstSelectedCard.getButton().repaint();
                secondSelectedCard.getButton().repaint();
                
                firstSelectedCard = null;
                secondSelectedCard = null;
            });
            flipBackTimer.setRepeats(false);
            flipBackTimer.start();
        }
        
        updateStatsLabels();
    }
    
    private void checkGameEnd() {
        if (pairsFound < 8) {
            showResult(false);
        }
    }
    
    private void showResult(boolean won) {
        if (won) {
            playSound(winSound);
            resultLabel.setText("YOU WON! 🎉");
            resultLabel.setForeground(new Color(0, 150, 0)); // Green
        } else {
            playSound(loseSound);
            resultLabel.setText("YOU LOST! 😢");
            resultLabel.setForeground(Color.RED);
        }
    }
    
    private void updateTimerLabel() {
        int minutes = timeRemaining / 60;
        int seconds = timeRemaining % 60;
        timerLabel.setText(String.format("⏱️ %d:%02d", minutes, seconds));
        
        // Change color to red when time is running out
        if (timeRemaining <= 10) {
            timerLabel.setForeground(Color.RED);
        } else {
            timerLabel.setForeground(TITLE_COLOR);
        }
    }
    
    private void updateStatsLabels() {
        pairsLabel.setText(String.format("🍬 Pairs: %d/8", pairsFound));
    }
    
    // Update the Card class to use images instead of colors
    private static class Card {
        private final Image candyImage;
        private final int candyImageIndex;
        private boolean matched;
        private boolean revealed;
        private JButton button;
        private int number;
        
        public Card(Image candyImage, int candyImageIndex) {
            this.candyImage = candyImage;
            this.candyImageIndex = candyImageIndex;
            this.matched = false;
            this.revealed = false;
            this.number = 0;
        }
        
        public Image getCandyImage() { return candyImage; }
        public int getCandyImageIndex() { return candyImageIndex; }
        public boolean isMatched() { return matched; }
        public boolean isRevealed() { return revealed; }
        public JButton getButton() { return button; }
        public int getNumber() { return number; }
        
        public void setMatched(boolean matched) { 
            this.matched = matched;
            if (button != null) {
                button.setEnabled(!matched);
            }
        }
        
        public void setRevealed(boolean revealed) { 
            this.revealed = revealed;
        }
        
        public void setButton(JButton button) { this.button = button; }
        public void setNumber(int number) { this.number = number; }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                new CandyMemoryGame();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}