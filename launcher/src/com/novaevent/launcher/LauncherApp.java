package com.novaevent.launcher;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JScrollBar;
import javax.swing.JEditorPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.Document;
import javax.swing.text.rtf.RTFEditorKit;
import java.awt.BorderLayout;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Taskbar;
import java.awt.Window;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import javax.swing.Timer;

public final class LauncherApp {
    private enum PageView {
        HOME,
        SETTINGS,
        NEWS
    }

    private enum SettingsTabView {
        GENERAL,
        CLIENT,
        ACCOUNT
    }

    private static final Gson GSON = new Gson();
    private static final Color APP_BG = new Color(30, 30, 30);
    private static final Color SIDEBAR_BG = new Color(49, 45, 45);
    private static final Color PANEL_BLACK = new Color(25, 25, 25);
    private static final Color INPUT_BG = new Color(36, 36, 36);
    private static final Color BORDER = new Color(62, 62, 62);
    private static final Color TEXT_PRIMARY = Color.WHITE;
    private static final Color TEXT_MUTED = new Color(194, 194, 194);
    private static final Color ACCENT = new Color(79, 168, 56);
    private static final Color ACCENT_DARK = new Color(45, 116, 31);
    private static final Color ACCENT_HOVER = new Color(93, 184, 68);
    private static final Color LOG_BG = new Color(19, 19, 19);
    private static final Color LOG_TEXT = new Color(222, 228, 222);
    private static final Color CARD_BG = new Color(26, 26, 26);
    private static final Color CARD_ACTIVE_BG = new Color(36, 36, 36);
    private static final String NEWS_DOCUMENT_URL = "https://drive.google.com/uc?export=download&id=1ukbGSaA0Q4QQMNmyol_bh1Pxd9-gjTML";

    private final Path projectDir;
    private final Path configPath;
    private final Path instancesDir;
    private final JTextArea logArea = new JTextArea();
    private final JToggleButton onlineModeButton = new JToggleButton("Онлайн (Ely.by)");
    private final JToggleButton offlineModeButton = new JToggleButton("Оффлайн (только ник)");
    private final JTextField usernameField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final JTextField launcherBackgroundPathField = new JTextField();
    private final JTextField javaPathField = new JTextField();
    private final JTextField minecraftArgumentsField = new JTextField();
    private final JTextField launchWrapperCommandField = new JTextField();
    private final JTextField serverAddressField = new JTextField();
    private final JComboBox<String> javaRuntimeComboBox = new JComboBox<>(new String[]{
            "Авто",
            "Встроенная Java 17",
            "Встроенная Java 21",
            "Встроенная Java 25",
            "Пользовательский путь"
    });
    private final JComboBox<String> launcherLogModeComboBox = new JComboBox<>(new String[]{
            "Отключён",
            "Сохранять в файл"
    });
    private final JComboBox<String> launcherOnGameLaunchComboBox = new JComboBox<>(new String[]{
            "Ничего не делать",
            "Скрывать лаунчер",
            "Закрывать лаунчер"
    });
    private final JTextField clientBundleUrlField = new JTextField();
    private final JFormattedTextField minMemoryField = new JFormattedTextField();
    private final JFormattedTextField maxMemoryField = new JFormattedTextField();
    private final JFormattedTextField widthField = new JFormattedTextField();
    private final JFormattedTextField heightField = new JFormattedTextField();
    private final JCheckBox fullscreenCheckBox = new JCheckBox("Полноэкранный режим");
    private final JCheckBox autoConnectCheckBox = new JCheckBox("Автоматически подключаться после запуска");
    private final JButton launchButton;
    private final JButton addClientButton = new JButton("+");
    private final JButton renameClientButton = new JButton("R");
    private final JButton deleteClientButton = new JButton("-");
    private final JButton modSyncButton = new JButton("SYNC");
    private final JButton accountButton = new JButton("АККАУНТ");
    private final JButton settingsButton = new JButton("НАСТРОЙКИ");
    private final JButton logToggleButton = new JButton("Показать логи");
    private final JButton bundlePauseButton = new JButton("Пауза");
    private final JButton bundleCancelButton = new JButton("Отмена");
    private final JButton checkSkinButton = new JButton("Проверить скин Ely");
    private final JButton openElyProfileButton = new JButton("Открыть скин Ely");
    private final JButton copyUuidButton = new JButton("Копировать UUID");
    private final JButton accountLogoutButton = new JButton("Выйти");
    private final JButton accountLoginButton = new JButton("Войти");
    private final JButton browseBackgroundButton = new JButton("Обзор...");
    private final JPanel clientListPanel = new JPanel();
    private final JPanel logContainer = new JPanel(new BorderLayout());
    private final JPanel bundleTransferPanel = new JPanel(new BorderLayout(12, 10));
    private JScrollPane settingsScrollPane;
    private final JLabel bundleTransferStatusLabel = new JLabel("Загрузка сборки не активна");
    private final JLabel bundleTransferDetailLabel = new JLabel(" ");
    private final JProgressBar bundleTransferProgressBar = new JProgressBar();
    private final JLabel accountModeStatusLabel = new JLabel();
    private final JPanel accountFieldsPanel = new JPanel(new GridBagLayout());
    private final JLabel skinCheckStatusLabel = new JLabel();
    private final JLabel skinCheckUuidLabel = new JLabel();
    private final JLabel skinCheckProfileLabel = new JLabel();
    private final JLabel selectedClientTileTitle = new JLabel();
    private final JLabel selectedClientTileSubtitle = new JLabel();
    private final JLabel installSourceTileSubtitle = new JLabel();
    private final JLabel playClientHeaderLabel = new JLabel("ВЫБРАННАЯ УСТАНОВКА");
    private final JLabel playClientNameLabel = new JLabel();
    private final JLabel playClientVersionLabel = new JLabel();
    private final JLabel playClientHintLabel = new JLabel();
    private final JLabel serverNameLabel = new JLabel("Сервер не указан");
    private final JLabel serverPlayersLabel = new JLabel("Игроки: -");
    private final JLabel serverPingLabel = new JLabel("Пинг: -");
    private final JLabel serverAddressLabel = new JLabel("Адрес: -");
    private final JEditorPane newsContentPane = new JEditorPane();
    private final SkinPreviewPanel skinPreviewPanel = new SkinPreviewPanel();
    private final JPanel accountAvatarPanel;
    private final JPanel serverStatusIconPanel;
    private final JPanel centerContentHost = new JPanel(new BorderLayout());
    private final BufferedImage logoImage;
    private final BufferedImage bannerImage;
    private BufferedImage currentBannerImage;
    private final BufferedImage appIconImage;
    private final BufferedImage gameIconImage;
    private final BufferedImage newsIconImage;
    private final BufferedImage playButtonImage;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    private LauncherConfig config;
    private JFrame frame;
    private Timer profileSaveTimer;
    private JDialog accountDialog;
    private Dimension accountDialogBaseSize = new Dimension(860, 560);
    private final Dimension settingsDialogBaseSize = new Dimension(1240, 780);
    private volatile String skinPreviewRequestKey = "";
    private volatile GoogleDriveInstaller.TransferControl activeBundleTransferControl;
    private volatile BufferedImage accountSkinHeadImage;
    private volatile BufferedImage serverIconImage;
    private volatile boolean newsLoading;
    private volatile String newsHtml = "";
    private volatile boolean launcherWindowHiddenForGame;
    private volatile String serverStatusRequestKey = "";
    private volatile boolean launcherUpdateCheckStarted;
    private PageView currentPageView = PageView.HOME;
    private SettingsTabView currentSettingsTabView = SettingsTabView.GENERAL;

    public LauncherApp(Path projectDir) {
        this.projectDir = projectDir;
        this.configPath = projectDir.resolve("launcher").resolve("launcher-config.json");
        this.instancesDir = projectDir.resolve("launcher").resolve("instances");
        this.config = LauncherConfig.load(configPath);
        this.config.ensureDefaults();
        this.logoImage = loadImageResource("assets/logo-minecraft-rendered.png", projectDir.resolve("launcher").resolve("assets").resolve("logo-minecraft-rendered.png"));
        BufferedImage configuredDefaultBanner = loadImageResource("assets/back.png", projectDir.resolve("back.png"));
        this.bannerImage = configuredDefaultBanner != null
                ? configuredDefaultBanner
                : loadImageResource("assets/banner-main.jpg", projectDir.resolve("launcher").resolve("assets").resolve("banner-main.jpg"));
        this.appIconImage = loadImageResource(
                "assets/official/official-launcher-icon.png",
                projectDir.resolve("launcher").resolve("assets").resolve("official").resolve("official-launcher-icon.png")
        );
        this.gameIconImage = loadImageResource(
                "assets/official/official-java-grass.png",
                projectDir.resolve("launcher").resolve("assets").resolve("official").resolve("official-java-grass.png")
        );
        this.newsIconImage = loadImageResource(
                "assets/official/official-java-creeper.png",
                projectDir.resolve("launcher").resolve("assets").resolve("official").resolve("official-java-creeper.png")
        );
        this.playButtonImage = loadImageResource("assets/butom.png", projectDir.resolve("butom.png"));
        this.currentBannerImage = resolveConfiguredBannerImage(this.config.launcherBackgroundPath);
        this.launchButton = new LauncherButton("ИГРАТЬ", playButtonImage);
        this.accountAvatarPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                Graphics2D g2 = (Graphics2D) graphics.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                int size = Math.min(getWidth(), getHeight());
                int drawX = (getWidth() - size) / 2;
                int drawY = (getHeight() - size) / 2;
                g2.setClip(new java.awt.geom.Ellipse2D.Float(drawX, drawY, size, size));
                BufferedImage avatarImage = accountSkinHeadImage != null ? accountSkinHeadImage : gameIconImage;
                if (avatarImage != null) {
                    g2.drawImage(avatarImage, drawX, drawY, size, size, null);
                } else {
                    g2.setColor(new Color(101, 176, 84));
                    g2.fillOval(drawX, drawY, size, size);
                }
                g2.dispose();
            }
        };
        this.accountAvatarPanel.setOpaque(false);
        this.accountAvatarPanel.setBorder(null);
        this.accountAvatarPanel.setPreferredSize(new Dimension(31, 31));
        this.accountAvatarPanel.setMinimumSize(new Dimension(31, 31));
        this.accountAvatarPanel.setMaximumSize(new Dimension(31, 31));
        this.serverStatusIconPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                Graphics2D g2 = (Graphics2D) graphics.create();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                int size = Math.min(getWidth(), getHeight());
                int drawX = (getWidth() - size) / 2;
                int drawY = (getHeight() - size) / 2;
                BufferedImage icon = serverIconImage != null ? serverIconImage : gameIconImage;
                if (icon != null) {
                    g2.drawImage(icon, drawX, drawY, size, size, null);
                } else {
                    g2.setColor(new Color(46, 46, 46));
                    g2.fillRect(drawX, drawY, size, size);
                }
                g2.dispose();
            }
        };
        this.serverStatusIconPanel.setOpaque(false);
        this.serverStatusIconPanel.setPreferredSize(new Dimension(54, 54));
        this.serverStatusIconPanel.setMinimumSize(new Dimension(54, 54));
        this.serverStatusIconPanel.setMaximumSize(new Dimension(54, 54));
    }

    public static void main(String[] args) {
        Path projectDir = resolveProjectDir(args);
        SwingUtilities.invokeLater(() -> new LauncherApp(projectDir).show());
    }

    private static Path resolveProjectDir(String[] args) {
        if (args.length > 0 && !args[0].isBlank()) {
            return Path.of(args[0]).toAbsolutePath().normalize();
        }

        try {
            Path codeSource = Path.of(LauncherApp.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI());
            Path maybeProjectDir;
            if (Files.isRegularFile(codeSource)) {
                maybeProjectDir = codeSource.getParent().getParent().getParent();
            } else {
                maybeProjectDir = codeSource.getParent().getParent().getParent();
            }
            if (maybeProjectDir != null && Files.isDirectory(maybeProjectDir.resolve("launcher"))) {
                return maybeProjectDir.toAbsolutePath().normalize();
            }
        } catch (URISyntaxException | NullPointerException ignored) {
        }

        return Path.of(".").toAbsolutePath().normalize();
    }

    private void show() {
        installLookAndFeel();
        config.ensureDefaults();
        loadGlobalConfigIntoFields();
        loadSelectedProfileIntoFields();

        styleField(usernameField);
        styleField(passwordField);
        styleField(launcherBackgroundPathField);
        styleField(javaPathField);
        styleField(minecraftArgumentsField);
        styleField(launchWrapperCommandField);
        styleField(serverAddressField);
        styleComboBox(launcherLogModeComboBox);
        styleComboBox(launcherOnGameLaunchComboBox);
        styleComboBox(javaRuntimeComboBox);
        styleField(clientBundleUrlField);
        styleField(minMemoryField);
        styleField(maxMemoryField);
        styleField(widthField);
        styleField(heightField);
        styleCheckBox(fullscreenCheckBox);
        styleCheckBox(autoConnectCheckBox);
        styleModeToggle(onlineModeButton);
        styleModeToggle(offlineModeButton);
        styleLaunchButton();
        styleAddClientButton();
        styleWideSidebarActionButton(modSyncButton);
        styleSideActionButton(renameClientButton);
        styleSideActionButton(deleteClientButton);
        styleToolbarButton(accountButton);
        styleSettingsButton();
        styleToolbarButton(logToggleButton);
        styleDialogButton(bundlePauseButton, new Color(66, 66, 66), new Color(130, 130, 130));
        styleDialogButton(bundleCancelButton, new Color(66, 66, 66), new Color(130, 130, 130));
        bundlePauseButton.addActionListener(event -> toggleBundleTransferPause());
        bundleCancelButton.addActionListener(event -> cancelBundleTransfer());
        accountButton.setIcon(new UserIcon(16, Color.WHITE));
        accountButton.setIconTextGap(10);
        settingsButton.setIcon(new GearIcon(16, Color.WHITE));
        settingsButton.setIconTextGap(10);
        logToggleButton.addActionListener(event -> toggleLogs());
        styleDialogButton(checkSkinButton, new Color(66, 66, 66), new Color(130, 130, 130));
        checkSkinButton.addActionListener(event -> checkElySkinAsync());
        styleDialogButton(openElyProfileButton, new Color(66, 66, 66), new Color(130, 130, 130));
        styleDialogButton(copyUuidButton, new Color(66, 66, 66), new Color(130, 130, 130));
        styleDialogButton(accountLogoutButton, new Color(66, 66, 66), new Color(130, 130, 130));
        styleDialogButton(accountLoginButton, ACCENT, ACCENT_DARK);
        accountLogoutButton.addActionListener(event -> logoutCurrentAccount());
        accountLoginButton.addActionListener(event -> authenticateAccountAsync());
        styleDialogButton(browseBackgroundButton, new Color(66, 66, 66), new Color(130, 130, 130));
        browseBackgroundButton.addActionListener(event -> chooseLauncherBackground());
        openElyProfileButton.addActionListener(event -> openSavedElyProfile());
        copyUuidButton.addActionListener(event -> copySavedUuid());

        launchButton.addActionListener(event -> launchAsync());
        addClientButton.addActionListener(event -> addClientProfile());
        modSyncButton.addActionListener(event -> syncModSyncAsync());
        renameClientButton.addActionListener(event -> renameSelectedProfile());
        deleteClientButton.addActionListener(event -> deleteSelectedProfile());
        accountButton.addActionListener(event -> openAccountDialog());
        settingsButton.addActionListener(event -> setCurrentPageView(PageView.SETTINGS));
        onlineModeButton.addActionListener(event -> setOfflineMode(false));
        offlineModeButton.addActionListener(event -> setOfflineMode(true));

        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        logArea.setLineWrap(false);
        logArea.setWrapStyleWord(false);
        logArea.setBackground(LOG_BG);
        logArea.setForeground(LOG_TEXT);
        logArea.setCaretColor(LOG_TEXT);
        logArea.setBorder(new EmptyBorder(8, 10, 8, 10));

        frame = new JFrame(LauncherMetadata.DISPLAY_NAME);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        if (appIconImage != null) {
            frame.setIconImage(appIconImage);
            try {
                if (Taskbar.isTaskbarSupported()) {
                    Taskbar.getTaskbar().setIconImage(appIconImage);
                }
            } catch (Exception ignored) {
            }
        }
        frame.setContentPane(new RootPanel());
        frame.setLayout(new BorderLayout());
        frame.add(createSidebar(), BorderLayout.WEST);
        frame.add(createCenterArea(), BorderLayout.CENTER);
        frame.setMinimumSize(new Dimension(1360, 880));
        restoreLauncherWindowBounds();
        installLauncherWindowPersistence();
        refreshProfileUi();
        updateAuthModeUi();
        refreshSkinPreviewAsync();
        frame.setVisible(true);
        checkLauncherUpdatesAsync();

        log("Launcher directory: " + projectDir.resolve("launcher"));
        log("Instances directory: " + instancesDir);
        log("Selected client directory: " + getSelectedGameDir());
    }

    private void restoreLauncherWindowBounds() {
        int width = Math.max(1360, config.launcherWindowWidth);
        int height = Math.max(880, config.launcherWindowHeight);
        frame.setSize(width, height);
        if (config.launcherWindowX == Integer.MIN_VALUE || config.launcherWindowY == Integer.MIN_VALUE) {
            frame.setLocationRelativeTo(null);
        } else {
            frame.setLocation(config.launcherWindowX, config.launcherWindowY);
        }
    }

    private void installLauncherWindowPersistence() {
        frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent event) {
                persistLauncherWindowBounds();
            }

            @Override
            public void componentResized(ComponentEvent event) {
                persistLauncherWindowBounds();
            }
        });
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                persistLauncherWindowBounds();
            }
        });
    }

    private void persistLauncherWindowBounds() {
        if (frame == null) {
            return;
        }
        config.launcherWindowWidth = frame.getWidth();
        config.launcherWindowHeight = frame.getHeight();
        config.launcherWindowX = frame.getX();
        config.launcherWindowY = frame.getY();
        scheduleConfigSave();
    }

    private void checkLauncherUpdatesAsync() {
        if (launcherUpdateCheckStarted) {
            return;
        }
        launcherUpdateCheckStarted = true;
        Thread thread = new Thread(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(java.net.URI.create(LauncherMetadata.LATEST_RELEASE_API_URL))
                        .timeout(Duration.ofSeconds(12))
                        .header("Accept", "application/vnd.github+json")
                        .header("User-Agent", LauncherMetadata.USER_AGENT)
                        .GET()
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    log("Launcher update check failed with HTTP " + response.statusCode());
                    return;
                }

                JsonObject release = GSON.fromJson(response.body(), JsonObject.class);
                if (release == null) {
                    return;
                }
                String latestTag = safeTrim(stringValue(release, "tag_name"));
                String releaseUrl = safeTrim(stringValue(release, "html_url"));
                if (latestTag.isBlank()) {
                    return;
                }
                if (isNewerVersion(latestTag, LauncherMetadata.VERSION)) {
                    log("Launcher update available: " + latestTag + " (current " + LauncherMetadata.VERSION + ")");
                    SwingUtilities.invokeLater(() -> promptLauncherUpdate(latestTag, releaseUrl.isBlank() ? LauncherMetadata.RELEASES_URL : releaseUrl));
                }
            } catch (Exception ex) {
                log("Launcher update check skipped: " + ex.getMessage());
            }
        }, "launcher-update-check");
        thread.setDaemon(true);
        thread.start();
    }

    private void promptLauncherUpdate(String latestTag, String releaseUrl) {
        if (frame == null || !frame.isDisplayable()) {
            return;
        }
        int option = JOptionPane.showConfirmDialog(
                frame,
                "Доступна новая версия launcher-а: " + latestTag + "\nОткрыть страницу релиза?",
                "Обновление launcher-а",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE
        );
        if (option == JOptionPane.YES_OPTION) {
            openUrlInBrowser(releaseUrl);
        }
    }

    private boolean isNewerVersion(String candidateVersion, String currentVersion) {
        int[] candidate = normalizeVersion(candidateVersion);
        int[] current = normalizeVersion(currentVersion);
        int max = Math.max(candidate.length, current.length);
        for (int index = 0; index < max; index++) {
            int left = index < candidate.length ? candidate[index] : 0;
            int right = index < current.length ? current[index] : 0;
            if (left > right) {
                return true;
            }
            if (left < right) {
                return false;
            }
        }
        return false;
    }

    private int[] normalizeVersion(String rawVersion) {
        String cleaned = safeTrim(rawVersion).replaceFirst("^[vV]", "");
        if (cleaned.isBlank()) {
            return new int[]{0};
        }
        String[] parts = cleaned.split("[^0-9]+");
        int[] normalized = new int[Math.max(1, parts.length)];
        int count = 0;
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                continue;
            }
            try {
                normalized[count++] = Integer.parseInt(part);
            } catch (NumberFormatException ignored) {
            }
        }
        if (count == 0) {
            return new int[]{0};
        }
        if (count == normalized.length) {
            return normalized;
        }
        int[] trimmed = new int[count];
        System.arraycopy(normalized, 0, trimmed, 0, count);
        return trimmed;
    }

    private String stringValue(JsonObject object, String property) {
        if (object == null || property == null || !object.has(property) || object.get(property).isJsonNull()) {
            return "";
        }
        return object.get(property).getAsString();
    }

    private void openUrlInBrowser(String url) {
        try {
            if (!Desktop.isDesktopSupported()) {
                log("Desktop browse is not supported on this system.");
                return;
            }
            Desktop.getDesktop().browse(java.net.URI.create(url));
        } catch (Exception ex) {
            log("Could not open URL: " + ex.getMessage());
        }
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setBackground(SIDEBAR_BG);
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(22, 22, 22)));
        sidebar.setPreferredSize(new Dimension(258, 100));

        JPanel topAccount = createSidebarAccountBlock();
        JPanel home = createSidebarNavButton("\u2302", "Главная", currentPageView == PageView.HOME, () -> setCurrentPageView(PageView.HOME));

        clientListPanel.setOpaque(false);
        clientListPanel.setLayout(new BoxLayout(clientListPanel, BoxLayout.Y_AXIS));

        sidebar.add(topAccount);
        sidebar.add(home);
        sidebar.add(clientListPanel);
        sidebar.add(Box.createVerticalGlue());
        sidebar.add(createSidebarActionStrip());
        sidebar.add(createSidebarNavButton("\u25a6", "Что нового", currentPageView == PageView.NEWS, () -> setCurrentPageView(PageView.NEWS)));
        sidebar.add(createSidebarNavButton("\u2630", "Настройки", currentPageView == PageView.SETTINGS, () -> setCurrentPageView(PageView.SETTINGS)));
        sidebar.add(createSidebarVersionRow());

        rebuildClientList();
        return sidebar;
    }

    private JPanel createSidebarAccountBlock() {
        JPanel block = new JPanel(new BorderLayout(10, 0));
        block.setOpaque(true);
        block.setBackground(new Color(60, 55, 55));
        block.setMaximumSize(new Dimension(Integer.MAX_VALUE, 74));
        block.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(15, 15, 15)),
                new EmptyBorder(11, 15, 11, 15)
        ));

        JPanel avatarWrap = new JPanel(new BorderLayout());
        avatarWrap.setOpaque(false);
        avatarWrap.setPreferredSize(new Dimension(31, 31));
        avatarWrap.setMinimumSize(new Dimension(31, 31));
        avatarWrap.setMaximumSize(new Dimension(31, 31));
        avatarWrap.add(accountAvatarPanel, BorderLayout.CENTER);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel name = new JLabel((config.username == null || config.username.isBlank())
                ? "Player"
                : config.username);
        name.setForeground(Color.WHITE);
        name.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        JLabel type = new JLabel(config.offlineMode ? "Локальный аккаунт" : "Аккаунт Ely.by");
        type.setForeground(new Color(172, 172, 172));
        type.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        text.add(name);
        text.add(Box.createVerticalStrut(2));
        text.add(type);

        JLabel chevron = new JLabel("⌄");
        chevron.setForeground(new Color(176, 176, 176));
        chevron.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 17));
        chevron.setBorder(new EmptyBorder(0, 0, 1, 0));

        block.add(avatarWrap, BorderLayout.WEST);
        block.add(text, BorderLayout.CENTER);
        block.add(chevron, BorderLayout.EAST);
        block.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        block.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                showAccountMenu(block);
            }
        });
        return block;
    }

    private void showAccountMenu(Component invoker) {
        int menuWidth = invoker.getWidth();
        int rowHeight = 54;
        int accountRowHeight = 64;
        int menuHeight = (rowHeight * 4) + accountRowHeight + 2;
        JPopupMenu popup = new JPopupMenu();
        popup.setOpaque(true);
        popup.setBackground(new Color(63, 58, 58));
        popup.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(225, 225, 225), 1, false),
                new EmptyBorder(0, 0, 0, 0)
        ));
        popup.setLayout(new BorderLayout());
        popup.setPopupSize(new Dimension(menuWidth, menuHeight));

        JPanel content = new JPanel();
        content.setOpaque(true);
        content.setBackground(new Color(63, 58, 58));
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setPreferredSize(new Dimension(menuWidth, menuHeight));
        content.setMinimumSize(new Dimension(menuWidth, menuHeight));
        content.setMaximumSize(new Dimension(menuWidth, menuHeight));

        content.add(createAccountMenuRow(menuWidth, "СПРАВКА", null, () -> {
            popup.setVisible(false);
            openAccountDialog();
        }));
        content.add(createAccountMenuRow(menuWidth, "УПРАВЛЕНИЕ ПРОФИЛЕМ", "↗", () -> {
            popup.setVisible(false);
            openElyAccountManagement();
        }));
        content.add(createAccountMenuRow(menuWidth, "ВЫЙТИ", null, () -> {
            popup.setVisible(false);
            logoutCurrentAccount();
        }));
        content.add(createAccountMenuCurrentAccountRow(menuWidth, popup));
        content.add(createAccountMenuRow(menuWidth, "ВСЕ АККАУНТЫ", null, () -> {
            popup.setVisible(false);
            openAccountDialog();
        }));

        popup.add(content, BorderLayout.CENTER);

        popup.show(invoker, 0, invoker.getHeight());
    }

    private JPanel createAccountMenuRow(int menuWidth, String title, String trailing, Runnable action) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(true);
        row.setBackground(new Color(63, 58, 58));
        row.setBorder(new EmptyBorder(0, 0, 0, 0));
        row.setMinimumSize(new Dimension(menuWidth, 54));
        row.setMaximumSize(new Dimension(menuWidth, 54));
        row.setPreferredSize(new Dimension(menuWidth, 54));

        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(14, 22, 14, 18));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        content.add(titleLabel, BorderLayout.WEST);

        if (trailing != null && !trailing.isBlank()) {
            JLabel trailingLabel = new JLabel(trailing);
            trailingLabel.setForeground(Color.WHITE);
            trailingLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
            content.add(trailingLabel, BorderLayout.EAST);
        }

        row.add(content, BorderLayout.CENTER);
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        row.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                row.setBackground(new Color(71, 66, 66));
            }

            @Override
            public void mouseExited(MouseEvent event) {
                row.setBackground(new Color(63, 58, 58));
            }

            @Override
            public void mousePressed(MouseEvent event) {
                action.run();
            }
        });
        return row;
    }

    private JPanel createAccountMenuCurrentAccountRow(int menuWidth, JPopupMenu popup) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(true);
        row.setBackground(new Color(63, 58, 58));
        row.setBorder(new EmptyBorder(0, 0, 0, 0));
        row.setMinimumSize(new Dimension(menuWidth, 64));
        row.setMaximumSize(new Dimension(menuWidth, 64));
        row.setPreferredSize(new Dimension(menuWidth, 64));

        JPanel content = new JPanel(new BorderLayout(12, 0));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(12, 22, 12, 18));

        JPanel avatarWrap = new JPanel(new BorderLayout());
        avatarWrap.setOpaque(false);
        avatarWrap.setPreferredSize(new Dimension(30, 30));
        avatarWrap.setMinimumSize(new Dimension(30, 30));
        avatarWrap.setMaximumSize(new Dimension(30, 30));
        avatarWrap.add(createAccountAvatarPreview(30), BorderLayout.CENTER);

        JLabel nameLabel = new JLabel(safeTrim(config.savedProfileName).isBlank() ? safeTrim(config.username) : safeTrim(config.savedProfileName));
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));

        JLabel checkLabel = new JLabel("✓");
        checkLabel.setForeground(Color.WHITE);
        checkLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));

        content.add(avatarWrap, BorderLayout.WEST);
        content.add(nameLabel, BorderLayout.CENTER);
        content.add(checkLabel, BorderLayout.EAST);
        row.add(content, BorderLayout.CENTER);
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        row.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                row.setBackground(new Color(71, 66, 66));
            }

            @Override
            public void mouseExited(MouseEvent event) {
                row.setBackground(new Color(63, 58, 58));
            }

            @Override
            public void mousePressed(MouseEvent event) {
                popup.setVisible(false);
            }
        });
        return row;
    }

    private JPanel createAccountAvatarPreview(int size) {
        JPanel avatar = new JPanel() {
            @Override
            protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                Graphics2D g2 = (Graphics2D) graphics.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                int side = Math.min(getWidth(), getHeight());
                int drawX = (getWidth() - side) / 2;
                int drawY = (getHeight() - side) / 2;
                g2.setClip(new java.awt.geom.Ellipse2D.Float(drawX, drawY, side, side));
                BufferedImage avatarImage = accountSkinHeadImage != null ? accountSkinHeadImage : gameIconImage;
                if (avatarImage != null) {
                    g2.drawImage(avatarImage, drawX, drawY, side, side, null);
                } else {
                    g2.setColor(new Color(101, 176, 84));
                    g2.fillOval(drawX, drawY, side, side);
                }
                g2.dispose();
            }
        };
        avatar.setOpaque(false);
        avatar.setBorder(null);
        avatar.setPreferredSize(new Dimension(size, size));
        avatar.setMinimumSize(new Dimension(size, size));
        avatar.setMaximumSize(new Dimension(size, size));
        return avatar;
    }

    private JPanel createSidebarNavButton(String iconText, String titleText, boolean active, Runnable onClick) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(true);
        boolean footerRow = "Что нового".equals(titleText) || "Настройки".equals(titleText);
        boolean homeRow = "Главная".equals(titleText);
        Color activeBg = new Color(52, 47, 47);
        Color inactiveBg = footerRow ? new Color(57, 53, 53) : new Color(52, 48, 48);
        row.setBackground(active ? activeBg : inactiveBg);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, footerRow ? 59 : 61));
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(
                        active && homeRow ? 1 : 0,
                        0,
                        1,
                        0,
                        active && homeRow ? new Color(112, 108, 108) : new Color(17, 17, 17)
                ),
                new EmptyBorder(0, 0, 0, 0)
        ));

        JPanel leftStrip = new JPanel();
        leftStrip.setOpaque(true);
        leftStrip.setBackground(active && homeRow ? new Color(246, 246, 246) : SIDEBAR_BG);
        leftStrip.setPreferredSize(new Dimension(active && homeRow ? 6 : 4, 1));
        row.add(leftStrip, BorderLayout.WEST);

        JPanel content = new JPanel(new BorderLayout(12, 0));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(14, 18, 14, 16));

        JComponent icon;
        if ("Что нового".equals(titleText)) {
            JLabel iconLabel = new JLabel(new NewsSidebarIcon(19, Color.WHITE));
            icon = iconLabel;
        } else if ("Главная".equals(titleText)) {
            JLabel iconLabel = new JLabel(new HomeSidebarIcon(20, Color.WHITE));
            icon = iconLabel;
        } else if ("Настройки".equals(titleText)) {
            JLabel iconLabel = new JLabel(new SlidersSidebarIcon(19, Color.WHITE));
            icon = iconLabel;
        } else {
            JLabel iconLabel = new JLabel(iconText);
            iconLabel.setForeground(new Color(230, 230, 230));
            iconLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 18));
            icon = iconLabel;
        }

        String navLabel = titleText.toUpperCase(java.util.Locale.ROOT);
        JLabel title = new JLabel(navLabel);
        title.setForeground(TEXT_PRIMARY);
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));

        content.add(icon, BorderLayout.WEST);
        content.add(title, BorderLayout.CENTER);
        row.add(content, BorderLayout.CENTER);
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (onClick != null) {
            row.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent event) {
                    onClick.run();
                }
            });
        }
        return row;
    }

    private JPanel createSidebarVersionRow() {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(true);
        row.setBackground(new Color(47, 43, 43));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 23));
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(18, 18, 18)),
                new EmptyBorder(4, 10, 4, 8)
        ));

        JLabel version = new JLabel("v." + LauncherMetadata.PRODUCT_NAME + "-" + LauncherMetadata.VERSION);
        version.setForeground(new Color(132, 132, 132));
        version.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 7));
        row.add(version, BorderLayout.WEST);
        return row;
    }

    private JPanel createSidebarActionStrip() {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setOpaque(true);
        row.setBackground(new Color(47, 43, 43));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        row.setPreferredSize(new Dimension(258, 64));
        row.setMinimumSize(new Dimension(258, 64));
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 1, 0, new Color(17, 17, 17)),
                new EmptyBorder(10, 8, 10, 8)
        ));

        row.add(Box.createHorizontalGlue());
        row.add(addClientButton);
        row.add(Box.createHorizontalStrut(8));
        row.add(modSyncButton);
        row.add(Box.createHorizontalStrut(8));
        row.add(renameClientButton);
        row.add(Box.createHorizontalStrut(8));
        row.add(deleteClientButton);
        row.add(Box.createHorizontalGlue());
        return row;
    }

    private JPanel createSidebarEntry(ClientProfile profile, boolean active) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(true);
        Color activeBg = new Color(62, 57, 57);
        Color inactiveBg = new Color(52, 48, 48);
        row.setBackground(active ? activeBg : inactiveBg);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 61));
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(
                        0,
                        0,
                        1,
                        0,
                        new Color(17, 17, 17)
                ),
                new EmptyBorder(0, 0, 0, 0)
        ));
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JPanel leftStrip = new JPanel();
        leftStrip.setOpaque(true);
        leftStrip.setBackground(SIDEBAR_BG);
        leftStrip.setPreferredSize(new Dimension(4, 1));
        row.add(leftStrip, BorderLayout.WEST);

        JPanel left = new JPanel(new BorderLayout());
        left.setOpaque(false);
        left.setPreferredSize(new Dimension(58, 45));
        left.setMinimumSize(new Dimension(58, 45));
        left.setMaximumSize(new Dimension(58, 45));
        left.setBorder(new EmptyBorder(9, 18, 9, 0));

        JPanel iconWrap = new JPanel(new BorderLayout());
        iconWrap.setOpaque(false);
        iconWrap.setPreferredSize(new Dimension(30, 30));
        iconWrap.setMinimumSize(new Dimension(30, 30));
        iconWrap.setMaximumSize(new Dimension(30, 30));

        JPanel icon = new SidebarIconPanel(new Color(101, 176, 84), active, gameIconImage);
        icon.setPreferredSize(new Dimension(30, 30));
        icon.setMinimumSize(new Dimension(30, 30));
        icon.setMaximumSize(new Dimension(30, 30));
        iconWrap.add(icon, BorderLayout.CENTER);
        left.add(iconWrap, BorderLayout.CENTER);

        JPanel labelWrap = new JPanel();
        labelWrap.setOpaque(false);
        labelWrap.setLayout(new BoxLayout(labelWrap, BoxLayout.Y_AXIS));
        labelWrap.setBorder(new EmptyBorder(9, 0, 9, 12));

        JLabel eyebrow = new JLabel("MINECRAFT:");
        eyebrow.setForeground(new Color(186, 186, 186));
        eyebrow.setFont(new Font(Font.MONOSPACED, Font.BOLD, 8));
        eyebrow.setAlignmentX(Component.LEFT_ALIGNMENT);

        String rowTitle = profile.title == null ? "" : profile.title.toUpperCase(java.util.Locale.ROOT);
        JLabel title = new JLabel("<html><div style='width:134px;'>" + escapeHtml(rowTitle).replace("\n", "<br>") + "</div></html>");
        title.setForeground(TEXT_PRIMARY);
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        labelWrap.add(eyebrow);
        labelWrap.add(Box.createVerticalStrut(3));
        labelWrap.add(title);

        JPanel content = new JPanel(new BorderLayout(8, 0));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(0, 0, 0, 4));
        content.add(left, BorderLayout.WEST);
        content.add(labelWrap, BorderLayout.CENTER);
        row.add(content, BorderLayout.CENTER);

        row.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                selectProfile(profile.id);
                if (event.getClickCount() >= 2 && SwingUtilities.isLeftMouseButton(event)) {
                    renameSelectedProfile();
                }
            }
        });
        return row;
    }

    private JPanel createCenterArea() {
        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(true);
        center.setBackground(new Color(18, 18, 18));
        center.setBorder(new EmptyBorder(0, 0, 0, 0));
        centerContentHost.setOpaque(true);
        centerContentHost.setBackground(new Color(18, 18, 18));
        rebuildCenterContent();
        center.add(centerContentHost, BorderLayout.CENTER);
        return center;
    }

    private void rebuildCenterContent() {
        centerContentHost.removeAll();
        if (currentPageView == PageView.SETTINGS) {
            centerContentHost.add(createSettingsPageView(), BorderLayout.CENTER);
        } else if (currentPageView == PageView.NEWS) {
            centerContentHost.add(createNewsPageView(), BorderLayout.CENTER);
        } else {
            JPanel home = new JPanel(new BorderLayout());
            home.setOpaque(true);
            home.setBackground(new Color(18, 18, 18));
            home.add(createTopStage(), BorderLayout.CENTER);
            home.add(createGreenLowerPanel(), BorderLayout.SOUTH);
            centerContentHost.add(home, BorderLayout.CENTER);
        }
        centerContentHost.revalidate();
        centerContentHost.repaint();
    }

    private void setCurrentPageView(PageView pageView) {
        if (pageView == null || this.currentPageView == pageView) {
            return;
        }
        this.currentPageView = pageView;
        if (frame != null) {
            frame.getContentPane().removeAll();
            frame.add(createSidebar(), BorderLayout.WEST);
            frame.add(createCenterArea(), BorderLayout.CENTER);
            frame.revalidate();
            frame.repaint();
        }
    }

    private JPanel createTopStage() {
        JPanel stage = new JPanel(new BorderLayout());
        stage.setOpaque(true);
        stage.setBackground(new Color(20, 20, 20));
        stage.add(createBannerPanel(), BorderLayout.CENTER);
        stage.add(createPlayBar(), BorderLayout.SOUTH);
        return stage;
    }

    private JPanel createBannerPanel() {
        JPanel bannerWrap = new JPanel(new BorderLayout());
        bannerWrap.setOpaque(true);
        bannerWrap.setBackground(new Color(20, 20, 20));
        bannerWrap.setBorder(new EmptyBorder(0, 0, 0, 0));

        JPanel hero = new JPanel(new BorderLayout(0, 0));
        hero.setOpaque(true);
        hero.setBackground(new Color(20, 20, 20));

        JPanel banner = new ArtworkPanel(this::getCurrentBannerImage);
        banner.setLayout(new BorderLayout());
        banner.setBorder(new EmptyBorder(0, 0, 0, 0));

        JComponent logo = logoImage != null ? new LogoImagePanel(logoImage) : new MinecraftLogoPanel();
        logo.setPreferredSize(new Dimension(360, 150));
        logo.setMinimumSize(new Dimension(360, 150));
        logo.setMaximumSize(new Dimension(360, 150));
        JPanel logoWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        logoWrap.setOpaque(false);
        logoWrap.setBorder(new EmptyBorder(26, 0, 0, 0));
        logoWrap.add(logo);
        banner.add(logoWrap, BorderLayout.NORTH);

        JPanel storyWrap = new JPanel(new BorderLayout());
        storyWrap.setOpaque(false);
        storyWrap.setBorder(new EmptyBorder(0, 26, 22, 0));
        storyWrap.add(createHeroStoryCard(), BorderLayout.SOUTH);
        banner.add(storyWrap, BorderLayout.WEST);

        JPanel serverWrap = new JPanel(new BorderLayout());
        serverWrap.setOpaque(false);
        serverWrap.setBorder(new EmptyBorder(26, 0, 22, 22));
        serverWrap.add(createServerStatusCard(), BorderLayout.NORTH);
        banner.add(serverWrap, BorderLayout.EAST);

        hero.add(banner, BorderLayout.CENTER);

        bannerWrap.add(hero, BorderLayout.CENTER);
        return bannerWrap;
    }

    private JPanel createVersionTile(String header, JLabel dynamicTitle, JLabel dynamicSubtitle, boolean green) {
        JPanel tile = new JPanel();
        tile.setLayout(new BoxLayout(tile, BoxLayout.Y_AXIS));
        tile.setOpaque(true);
        tile.setBackground(green ? new Color(73, 151, 76) : new Color(16, 16, 16, 210));
        tile.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(255, 255, 255, 10), 1, false),
                new EmptyBorder(7, 11, 7, 11)
        ));
        tile.setMaximumSize(new Dimension(164, 36));
        tile.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel headerLabel = new JLabel(header);
        headerLabel.setForeground(new Color(198, 198, 198));
        headerLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 8));

        if (dynamicTitle != null) {
            dynamicTitle.setForeground(Color.WHITE);
            dynamicTitle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        }
        dynamicSubtitle.setForeground(new Color(180, 180, 180));
        dynamicSubtitle.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 8));

        tile.add(headerLabel);
        if (dynamicTitle != null) {
            tile.add(Box.createVerticalStrut(1));
            tile.add(dynamicTitle);
        }
        tile.add(dynamicSubtitle);
        return tile;
    }

    private JPanel createHeroStoryCard() {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setOpaque(true);
        card.setBackground(new Color(15, 15, 15, 230));
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(40, 40, 40), 1, false),
                new EmptyBorder(20, 22, 18, 22)
        ));
        card.setPreferredSize(new Dimension(328, 212));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setBorder(new EmptyBorder(0, 6, 0, 6));

        JLabel title = new JLabel("Запускай свои Forge-клиенты");
        title.setForeground(Color.WHITE);
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextArea body = new JTextArea("Единый лаунчер для твоих 1.20.1 сборок. Профили, Ely.by, Java, настройки и запуск из одного окна без лишней возни.");
        body.setEditable(false);
        body.setOpaque(false);
        body.setLineWrap(true);
        body.setWrapStyleWord(true);
        body.setForeground(new Color(214, 214, 214));
        body.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        body.setBorder(null);
        body.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.setMaximumSize(new Dimension(270, Integer.MAX_VALUE));

        text.add(title);
        text.add(Box.createVerticalStrut(8));
        text.add(body);

        JPanel action = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        action.setOpaque(false);
        action.setBorder(new EmptyBorder(0, 6, 0, 6));
        JButton readMore = new JButton("Открыть аккаунт");
        styleToolbarButton(readMore);
        readMore.addActionListener(event -> openAccountDialog());
        action.add(readMore);

        card.add(text, BorderLayout.CENTER);
        card.add(action, BorderLayout.SOUTH);
        return card;
    }

    private JPanel createServerStatusCard() {
        JPanel card = new JPanel(new BorderLayout(0, 12));
        card.setOpaque(true);
        card.setBackground(new Color(15, 15, 15, 232));
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(40, 40, 40), 1, false),
                new EmptyBorder(16, 16, 16, 16)
        ));
        card.setPreferredSize(new Dimension(270, 166));

        JPanel top = new JPanel(new BorderLayout(12, 0));
        top.setOpaque(false);
        top.add(serverStatusIconPanel, BorderLayout.WEST);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        JLabel eyebrow = new JLabel("СЕРВЕР");
        eyebrow.setForeground(new Color(177, 177, 177));
        eyebrow.setFont(new Font(Font.MONOSPACED, Font.BOLD, 9));

        serverNameLabel.setForeground(Color.WHITE);
        serverNameLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        serverPlayersLabel.setForeground(new Color(210, 210, 210));
        serverPlayersLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        serverPingLabel.setForeground(new Color(210, 210, 210));
        serverPingLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        serverAddressLabel.setForeground(new Color(160, 160, 160));
        serverAddressLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));

        text.add(eyebrow);
        text.add(Box.createVerticalStrut(4));
        text.add(serverNameLabel);
        text.add(Box.createVerticalStrut(6));
        text.add(serverPlayersLabel);
        text.add(Box.createVerticalStrut(3));
        text.add(serverPingLabel);
        text.add(Box.createVerticalStrut(6));
        text.add(serverAddressLabel);

        top.add(text, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        bottom.setOpaque(false);
        JButton openAccount = new JButton("Открыть сервер");
        styleToolbarButton(openAccount);
        openAccount.addActionListener(event -> openServerAddress());
        bottom.add(openAccount);

        card.add(top, BorderLayout.CENTER);
        card.add(bottom, BorderLayout.SOUTH);
        return card;
    }

    private JPanel createPlayBar() {
        JPanel bar = new JPanel(new GridBagLayout());
        bar.setBackground(new Color(34, 30, 30));
        bar.setBorder(new EmptyBorder(10, 14, 10, 14));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;

        JPanel leftProfile = new JPanel();
        leftProfile.setOpaque(false);
        leftProfile.setLayout(new BorderLayout(8, 0));
        leftProfile.setPreferredSize(new Dimension(340, 48));
        leftProfile.setMinimumSize(new Dimension(340, 48));

        JPanel iconWrap = new JPanel(new BorderLayout());
        iconWrap.setOpaque(false);
        iconWrap.setPreferredSize(new Dimension(34, 34));
        iconWrap.setMinimumSize(new Dimension(34, 34));
        iconWrap.setMaximumSize(new Dimension(34, 34));
        JPanel icon = new SidebarIconPanel(new Color(101, 176, 84), true, gameIconImage);
        icon.setPreferredSize(new Dimension(34, 34));
        iconWrap.add(icon, BorderLayout.CENTER);

        JPanel textStack = new JPanel();
        textStack.setOpaque(false);
        textStack.setLayout(new BoxLayout(textStack, BoxLayout.Y_AXIS));

        playClientHeaderLabel.setForeground(new Color(150, 150, 150));
        playClientHeaderLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 9));
        playClientNameLabel.setForeground(TEXT_PRIMARY);
        playClientNameLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        playClientVersionLabel.setForeground(new Color(186, 186, 186));
        playClientVersionLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        playClientHintLabel.setForeground(new Color(128, 128, 128));
        playClientHintLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));

        textStack.add(Box.createVerticalGlue());
        textStack.add(playClientHeaderLabel);
        textStack.add(Box.createVerticalStrut(2));
        textStack.add(playClientNameLabel);
        textStack.add(Box.createVerticalStrut(1));
        textStack.add(playClientVersionLabel);
        textStack.add(Box.createVerticalGlue());

        JLabel chevron = new JLabel(">");
        chevron.setForeground(new Color(166, 166, 166));
        chevron.setFont(new Font(Font.MONOSPACED, Font.BOLD, 18));
        chevron.setBorder(new EmptyBorder(0, 10, 0, 6));

        leftProfile.add(iconWrap, BorderLayout.WEST);
        leftProfile.add(textStack, BorderLayout.CENTER);
        leftProfile.add(chevron, BorderLayout.EAST);

        JPanel leftProfileWrap = new JPanel(new BorderLayout());
        leftProfileWrap.setOpaque(true);
        leftProfileWrap.setBackground(new Color(18, 18, 18));
        leftProfileWrap.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(58, 58, 58), 1, false),
                new EmptyBorder(8, 10, 8, 10)
        ));
        leftProfileWrap.add(leftProfile, BorderLayout.CENTER);

        JPanel centerButton = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        centerButton.setOpaque(false);
        centerButton.add(launchButton);

        JPanel rightSpacer = new JPanel();
        rightSpacer.setOpaque(false);
        Dimension sideSize = new Dimension(360, 1);
        leftProfileWrap.setPreferredSize(new Dimension(sideSize.width, leftProfileWrap.getPreferredSize().height));
        leftProfileWrap.setMinimumSize(new Dimension(sideSize.width, leftProfileWrap.getMinimumSize().height));
        rightSpacer.setPreferredSize(sideSize);
        rightSpacer.setMinimumSize(sideSize);

        gbc.gridx = 0;
        gbc.weightx = 0;
        bar.add(leftProfileWrap, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        bar.add(centerButton, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        bar.add(rightSpacer, gbc);
        return bar;
    }

    private JPanel createGreenLowerPanel() {
        JPanel lower = new JPanel(new BorderLayout(0, 10));
        lower.setBackground(new Color(12, 12, 12));
        lower.setBorder(new EmptyBorder(0, 14, 16, 14));
        lower.setPreferredSize(new Dimension(100, 188));

        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.getViewport().setBackground(LOG_BG);
        logScroll.setBorder(new LineBorder(new Color(34, 34, 34), 1, false));
        logScroll.setPreferredSize(new Dimension(100, 112));

        JPanel logSection = new JPanel(new BorderLayout(0, 8));
        logSection.setOpaque(true);
        logSection.setBackground(new Color(12, 12, 12));
        logSection.setBorder(new EmptyBorder(4, 0, 0, 0));

        JPanel logHeader = new JPanel(new BorderLayout(10, 0));
        logHeader.setOpaque(false);
        JPanel logHeaderLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        logHeaderLeft.setOpaque(false);
        logHeaderLeft.add(logToggleButton);
        logHeader.add(logHeaderLeft, BorderLayout.WEST);
        logHeader.add(createBundleTransferPanel(), BorderLayout.CENTER);

        logContainer.setOpaque(false);
        logContainer.add(logScroll, BorderLayout.CENTER);
        logContainer.setVisible(true);
        logToggleButton.setText("Скрыть логи");

        logSection.add(logHeader, BorderLayout.NORTH);
        logSection.add(logContainer, BorderLayout.CENTER);

        lower.add(logSection, BorderLayout.CENTER);
        return lower;
    }

    private JPanel createFeaturedContentCard(ClientProfile profile) {
        JPanel card = new JPanel(new BorderLayout(0, 12));
        card.setOpaque(true);
        card.setBackground(new Color(8, 8, 8));
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(34, 34, 34), 1, false),
                new EmptyBorder(14, 16, 14, 16)
        ));
        card.setPreferredSize(new Dimension(0, 120));

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        JLabel eyebrow = new JLabel("АКТИВНАЯ УСТАНОВКА");
        eyebrow.setForeground(new Color(177, 177, 177));
        eyebrow.setFont(new Font(Font.MONOSPACED, Font.BOLD, 9));

        JLabel title = new JLabel("<html><b>" + escapeHtml(profile.title) + "</b></html>");
        title.setForeground(Color.WHITE);
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));

        JLabel subtitle = new JLabel(profile.subtitle);
        subtitle.setForeground(new Color(198, 198, 198));
        subtitle.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));

        top.add(eyebrow);
        top.add(Box.createVerticalStrut(4));
        top.add(title);
        top.add(Box.createVerticalStrut(2));
        top.add(subtitle);

        JPanel bottom = new JPanel(new GridBagLayout());
        bottom.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.insets = new Insets(0, 0, 0, 10);

        bottom.add(createInfoMiniBlock("ИСТОЧНИК", profile.bundleUrl == null || profile.bundleUrl.isBlank()
                ? "launcher/instances"
                : "Google Drive"), gbc);

        gbc.gridx = 1;
        gbc.insets = new Insets(0, 0, 0, 12);
        bottom.add(createInfoMiniBlock("JAVA", switch (fromJavaRuntimeLabel((String) javaRuntimeComboBox.getSelectedItem())) {
            case "java17" -> "Java 17";
            case "java21" -> "Java 21";
            case "java25" -> "Java 25";
            case "custom" -> "Пользовательская";
            default -> "Авто";
        }), gbc);

        gbc.gridx = 2;
        gbc.insets = new Insets(0, 0, 0, 0);
        bottom.add(createInfoMiniBlock("ПУТЬ", getSelectedGameDir().getFileName().toString()), gbc);

        card.add(top, BorderLayout.NORTH);
        card.add(bottom, BorderLayout.CENTER);
        return card;
    }

    private JPanel createInfoMiniBlock(String titleText, String valueText) {
        JPanel block = new JPanel(new BorderLayout(0, 4));
        block.setOpaque(false);

        JLabel title = new JLabel(titleText);
        title.setForeground(new Color(150, 150, 150));
        title.setFont(new Font(Font.MONOSPACED, Font.BOLD, 9));

        JTextArea value = new JTextArea(valueText);
        value.setEditable(false);
        value.setOpaque(false);
        value.setLineWrap(true);
        value.setWrapStyleWord(true);
        value.setForeground(Color.WHITE);
        value.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
        value.setBorder(null);

        block.add(title, BorderLayout.NORTH);
        block.add(value, BorderLayout.CENTER);
        return block;
    }

    private JPanel createReferenceInfoCard(String eyebrow, String titleHtml, String bodyText) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setOpaque(true);
        card.setBackground(new Color(8, 8, 8));
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(34, 34, 34), 1, false),
                new EmptyBorder(12, 12, 12, 12)
        ));
        card.setPreferredSize(new Dimension(150, 120));

        JPanel head = new JPanel();
        head.setOpaque(false);
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));

        JLabel eyebrowLabel = new JLabel(eyebrow);
        eyebrowLabel.setForeground(new Color(177, 177, 177));
        eyebrowLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 9));

        JLabel title = new JLabel(titleHtml);
        title.setForeground(Color.WHITE);
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));

        JTextArea body = new JTextArea(bodyText);
        body.setEditable(false);
        body.setLineWrap(true);
        body.setWrapStyleWord(true);
        body.setOpaque(false);
        body.setForeground(new Color(204, 204, 204));
        body.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 9));
        body.setBorder(null);

        head.add(eyebrowLabel);
        head.add(Box.createVerticalStrut(4));
        head.add(title);
        card.add(head, BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private JPanel createHomeCardGrid() {
        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);
        grid.setBorder(new EmptyBorder(10, 0, 0, 0));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;
        gbc.insets = new Insets(0, 0, 0, 8);

        gbc.gridx = 0;
        gbc.weightx = 1;
        grid.add(createHomePromoCard("НОВОСТИ", "Новый клиент", "Главная сцена лаунчера и быстрый вход в мир."), gbc);

        gbc.gridx = 1;
        grid.add(createHomePromoCard("СБОРКА", "Forge 1.20.1", "Запуск, установка и параметры профиля."), gbc);

        gbc.gridx = 2;
        grid.add(createHomePromoCard("ELY.BY", "Аккаунт", "Сессия, скин и профиль Ely.by."), gbc);

        gbc.gridx = 3;
        gbc.insets = new Insets(0, 0, 0, 0);
        grid.add(createHomePromoCard("СИСТЕМА", "Java и память", "Java 17/21, память и fullscreen."), gbc);
        return grid;
    }

    private JPanel createHomeSearchBar() {
        JPanel search = new JPanel(new BorderLayout(0, 6));
        search.setOpaque(false);
        search.setBorder(new EmptyBorder(0, 0, 8, 0));

        JLabel label = new JLabel("ПОИСК");
        label.setForeground(new Color(188, 188, 188));
        label.setFont(new Font(Font.MONOSPACED, Font.BOLD, 9));

        JTextField field = new JTextField("Что вы ищете?");
        field.setEditable(false);
        field.setForeground(new Color(150, 150, 150));
        field.setBackground(new Color(18, 18, 18));
        field.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(79, 168, 56), 1, false),
                new EmptyBorder(8, 10, 8, 10)
        ));

        search.add(label, BorderLayout.NORTH);
        search.add(field, BorderLayout.CENTER);
        return search;
    }

    private JPanel createHomePromoCard(String eyebrow, String title, String body) {
        JPanel card = new JPanel(new BorderLayout(0, 0));
        card.setOpaque(true);
        card.setBackground(new Color(10, 10, 10));
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(34, 34, 34), 1, false),
                new EmptyBorder(0, 0, 0, 0)
        ));
        card.setPreferredSize(new Dimension(180, 196));

        JPanel preview = new ArtworkPanel(this::getCurrentBannerImage);
        preview.setPreferredSize(new Dimension(180, 118));
        preview.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(24, 24, 24)));

        JLabel eyebrowLabel = new JLabel(eyebrow);
        eyebrowLabel.setForeground(new Color(177, 177, 177));
        eyebrowLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 8));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));

        JTextArea bodyArea = new JTextArea(body);
        bodyArea.setEditable(false);
        bodyArea.setOpaque(false);
        bodyArea.setLineWrap(true);
        bodyArea.setWrapStyleWord(true);
        bodyArea.setForeground(new Color(188, 188, 188));
        bodyArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 9));
        bodyArea.setBorder(null);

        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.setOpaque(true);
        content.setBackground(new Color(10, 10, 10));
        content.setBorder(new EmptyBorder(10, 12, 12, 12));

        JPanel head = new JPanel();
        head.setOpaque(false);
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        head.add(eyebrowLabel);
        head.add(Box.createVerticalStrut(4));
        head.add(titleLabel);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        footer.setOpaque(false);
        JButton readMore = new JButton("Подробнее");
        readMore.setFocusPainted(false);
        readMore.setBackground(new Color(84, 170, 57));
        readMore.setForeground(Color.WHITE);
        readMore.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        readMore.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(30, 30, 30), 1, false),
                new EmptyBorder(6, 10, 6, 10)
        ));
        footer.add(readMore);

        content.add(head, BorderLayout.NORTH);
        content.add(bodyArea, BorderLayout.CENTER);
        content.add(footer, BorderLayout.SOUTH);

        card.add(preview, BorderLayout.NORTH);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private JPanel createBundleTransferPanel() {
        bundleTransferPanel.setOpaque(true);
        bundleTransferPanel.setBackground(new Color(14, 14, 14));
        bundleTransferPanel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(32, 32, 32), 1, false),
                new EmptyBorder(3, 6, 3, 6)
        ));
        bundleTransferPanel.setVisible(false);

        JPanel textPanel = new JPanel(new BorderLayout(0, 2));
        textPanel.setOpaque(false);

        bundleTransferStatusLabel.setForeground(Color.WHITE);
        bundleTransferStatusLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        bundleTransferDetailLabel.setForeground(new Color(205, 205, 205));
        bundleTransferDetailLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 8));

        textPanel.add(bundleTransferStatusLabel, BorderLayout.NORTH);
        textPanel.add(bundleTransferDetailLabel, BorderLayout.SOUTH);

        bundleTransferProgressBar.setForeground(ACCENT);
        bundleTransferProgressBar.setBackground(new Color(14, 14, 14));
        bundleTransferProgressBar.setBorder(new LineBorder(new Color(44, 44, 44), 1, false));
        bundleTransferProgressBar.setStringPainted(true);
        bundleTransferProgressBar.setPreferredSize(new Dimension(156, 8));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        controls.setOpaque(false);
        controls.add(bundlePauseButton);
        controls.add(bundleCancelButton);

        bundleTransferPanel.add(textPanel, BorderLayout.WEST);
        bundleTransferPanel.add(bundleTransferProgressBar, BorderLayout.CENTER);
        bundleTransferPanel.add(controls, BorderLayout.EAST);
        setBundleTransferIdle();
        return bundleTransferPanel;
    }

    private JPanel createSettingsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setOpaque(true);
        panel.setBackground(new Color(31, 28, 28));
        panel.setPreferredSize(new Dimension(1080, 100));

        JPanel header = new JPanel();
        header.setOpaque(true);
        header.setBackground(new Color(42, 38, 38));
        header.setBorder(new EmptyBorder(14, 22, 0, 22));
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("НАСТРОЙКИ");
        title.setForeground(Color.WHITE);
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel tabs = new JPanel(new FlowLayout(FlowLayout.LEFT, 28, 0));
        tabs.setOpaque(false);
        tabs.setAlignmentX(Component.LEFT_ALIGNMENT);
        tabs.setBorder(new EmptyBorder(8, 0, 0, 0));
        tabs.add(createSettingsTab("Основное", currentSettingsTabView == SettingsTabView.GENERAL, () -> switchSettingsTab(SettingsTabView.GENERAL)));
        tabs.add(createSettingsTab("Клиент", currentSettingsTabView == SettingsTabView.CLIENT, () -> switchSettingsTab(SettingsTabView.CLIENT)));
        tabs.add(createSettingsTab("Учётная запись", currentSettingsTabView == SettingsTabView.ACCOUNT, () -> switchSettingsTab(SettingsTabView.ACCOUNT)));

        header.add(title);
        header.add(tabs);

        JPanel body = new JPanel();
        body.setOpaque(true);
        body.setBackground(new Color(24, 24, 24));
        body.setLayout(new BorderLayout());
        body.setBorder(new EmptyBorder(30, 44, 30, 44));

        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setPreferredSize(new Dimension(720, 1200));

        JComboBox<String> languageComboBox = new JComboBox<>(new String[]{"Русский - Россия"});
        styleComboBox(languageComboBox);
        languageComboBox.setEnabled(true);

        if (currentSettingsTabView == SettingsTabView.GENERAL) {
            column.add(createSettingsBlockTitle("ИНТЕРФЕЙС ЛАУНЧЕРА"));
            column.add(Box.createVerticalStrut(12));
            column.add(createSettingsInlineRow("Язык", createOfficialField(languageComboBox, 420)));
            column.add(Box.createVerticalStrut(36));

            column.add(createSettingsBlockTitle("ФОН ЛАУНЧЕРА"));
            column.add(Box.createVerticalStrut(12));
            column.add(createLauncherBackgroundInlineRow());
            column.add(Box.createVerticalStrut(10));
            column.add(createSettingsLauncherHint("Поддерживаются изображения JPG и PNG. Фон заполняет область без растягивания: лишнее аккуратно обрезается."));
            column.add(Box.createVerticalStrut(36));

            column.add(createSettingsBlockTitle("ЖУРНАЛ СОБЫТИЙ"));
            column.add(Box.createVerticalStrut(12));
            column.add(createSettingsInlineRow("Журнал лаунчера", createOfficialField(launcherLogModeComboBox, 420)));
            column.add(Box.createVerticalStrut(36));

            column.add(createSettingsBlockTitle("ПОВЕДЕНИЕ ПРИ ЗАПУСКЕ"));
            column.add(Box.createVerticalStrut(12));
            column.add(createSettingsInlineRow("При запуске Minecraft", createOfficialField(launcherOnGameLaunchComboBox, 420)));
            column.add(Box.createVerticalStrut(36));
        }

        if (currentSettingsTabView == SettingsTabView.CLIENT) {
            column.add(createSettingsBlockTitle("ОКНО ИГРЫ"));
            column.add(Box.createVerticalStrut(12));
            column.add(createOfficialCheckRow(fullscreenCheckBox));
            column.add(Box.createVerticalStrut(14));
            column.add(createSettingsInlineRow("Ширина", createOfficialField(widthField, 250)));
            column.add(Box.createVerticalStrut(14));
            column.add(createSettingsInlineRow("Высота", createOfficialField(heightField, 250)));
            column.add(Box.createVerticalStrut(36));

            column.add(createSettingsBlockTitle("JAVA"));
            column.add(Box.createVerticalStrut(12));
            column.add(createSettingsInlineRow("Режим Java", createOfficialField(javaRuntimeComboBox, 420)));
            column.add(Box.createVerticalStrut(14));
            column.add(createSettingsInlineRow("Путь к Java", createOfficialField(javaPathField, 714)));
            column.add(Box.createVerticalStrut(14));
            column.add(createSettingsInlineRow("Аргументы Minecraft", createOfficialField(minecraftArgumentsField, 714)));
            column.add(Box.createVerticalStrut(14));
            column.add(createSettingsInlineRow("Команда-обёртка", createOfficialField(launchWrapperCommandField, 714)));
            column.add(Box.createVerticalStrut(36));

            column.add(createSettingsBlockTitle("ПАМЯТЬ"));
            column.add(Box.createVerticalStrut(12));
            column.add(createSettingsInlineRow("Мин. память (MB)", createOfficialField(minMemoryField, 250)));
            column.add(Box.createVerticalStrut(14));
            column.add(createSettingsInlineRow("Макс. память (MB)", createOfficialField(maxMemoryField, 250)));
            column.add(Box.createVerticalStrut(36));

            column.add(createSettingsBlockTitle("СЕРВЕР"));
            column.add(Box.createVerticalStrut(12));
            column.add(createSettingsInlineRow("Адрес сервера", createOfficialField(serverAddressField, 420)));
            column.add(Box.createVerticalStrut(14));
            column.add(createOfficialCheckRow(autoConnectCheckBox));
            column.add(Box.createVerticalStrut(10));
            column.add(createSettingsLauncherHint("Укажи адрес в формате host или host:port. Если автоподключение включено, launcher после запуска сам войдёт на сервер."));
            column.add(Box.createVerticalStrut(36));

            column.add(createSettingsBlockTitle("ИСТОЧНИК КЛИЕНТА"));
            column.add(Box.createVerticalStrut(12));
            column.add(createSettingsInlineRow("Google Drive URL", createOfficialField(clientBundleUrlField, 714)));
            column.add(Box.createVerticalStrut(36));

            column.add(createSettingsBlockTitle("ФАЙЛЫ КЛИЕНТА"));
            column.add(Box.createVerticalStrut(12));
            column.add(createClientFolderInlineRow());
            column.add(Box.createVerticalGlue());
        }

        if (currentSettingsTabView == SettingsTabView.ACCOUNT) {
            JPanel section = createAccountSection();
            section.setAlignmentX(Component.LEFT_ALIGNMENT);
            section.setMaximumSize(new Dimension(714, Integer.MAX_VALUE));
            column.add(section);
            column.add(Box.createVerticalGlue());
        }

        body.add(column, BorderLayout.WEST);

        JScrollPane scrollPane = new JScrollPane(body);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(new Color(24, 24, 24));
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);
        styleSettingsScrollPane(scrollPane);
        settingsScrollPane = scrollPane;
        SwingUtilities.invokeLater(() -> {
            JScrollBar bar = scrollPane.getVerticalScrollBar();
            if (bar != null) {
                bar.setValue(0);
            }
        });

        panel.add(header, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createSettingsPageView() {
        JPanel page = new JPanel(new BorderLayout());
        page.setOpaque(true);
        page.setBackground(new Color(24, 24, 24));

        JPanel panel = createSettingsPanel();
        panel.setBorder(new EmptyBorder(0, 0, 0, 0));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(true);
        actions.setBackground(new Color(24, 24, 24));
        actions.setBorder(new EmptyBorder(12, 0, 12, 18));

        JButton saveButton = new JButton("Сохранить");
        styleDialogButton(saveButton, new Color(79, 168, 56), new Color(39, 92, 28));
        saveButton.addActionListener(event -> {
            saveConfigQuietly();
            refreshProfileUi();
            setCurrentPageView(PageView.HOME);
        });

        JButton closeButton = new JButton("Назад");
        styleDialogButton(closeButton, new Color(66, 66, 66), new Color(130, 130, 130));
        closeButton.addActionListener(event -> setCurrentPageView(PageView.HOME));

        actions.add(saveButton);
        actions.add(closeButton);

        page.add(panel, BorderLayout.CENTER);
        page.add(actions, BorderLayout.SOUTH);
        return page;
    }

    private JPanel createNewsPageView() {
        JPanel page = new JPanel(new BorderLayout());
        page.setOpaque(true);
        page.setBackground(new Color(24, 24, 24));

        JPanel header = new JPanel();
        header.setOpaque(true);
        header.setBackground(new Color(42, 38, 38));
        header.setBorder(new EmptyBorder(14, 22, 14, 22));
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("ЧТО НОВОГО");
        title.setForeground(Color.WHITE);
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel hint = new JLabel("Новости и обновления лаунчера загружаются из удалённого файла");
        hint.setForeground(new Color(190, 190, 190));
        hint.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);

        header.add(title);
        header.add(Box.createVerticalStrut(6));
        header.add(hint);

        newsContentPane.setEditable(false);
        newsContentPane.setContentType("text/html");
        newsContentPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        newsContentPane.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        newsContentPane.setForeground(Color.WHITE);
        newsContentPane.setBackground(new Color(24, 24, 24));
        newsContentPane.setBorder(new EmptyBorder(26, 28, 26, 28));
        newsContentPane.setText(buildNewsLoadingHtml());

        JScrollPane scrollPane = new JScrollPane(newsContentPane);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(new Color(24, 24, 24));
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);
        styleSettingsScrollPane(scrollPane);

        page.add(header, BorderLayout.NORTH);
        page.add(scrollPane, BorderLayout.CENTER);

        ensureNewsLoadedAsync();
        return page;
    }

    private void ensureNewsLoadedAsync() {
        if (newsLoading) {
            return;
        }

        newsLoading = true;
        newsContentPane.setText(buildNewsLoadingHtml());

        Thread worker = new Thread(() -> {
            String html;
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(java.net.URI.create(NEWS_DOCUMENT_URL))
                        .timeout(Duration.ofSeconds(20))
                        .GET()
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (response.statusCode() / 100 != 2) {
                    throw new IOException("HTTP " + response.statusCode());
                }
                html = renderRemoteNewsToHtml(response.body());
                newsHtml = html;
            } catch (Exception ex) {
                html = buildNewsErrorHtml("Не удалось загрузить новости: " + ex.getMessage());
                log("Could not load launcher news: " + ex.getMessage());
            }
            newsLoading = false;
            String finalHtml = html;
            SwingUtilities.invokeLater(() -> {
                newsContentPane.setText(finalHtml);
                newsContentPane.setCaretPosition(0);
            });
        }, "launcher-news-loader");
        worker.setDaemon(true);
        worker.start();
    }

    private String renderRemoteNewsToHtml(String raw) {
        String source = normalizeNewsSource(raw);
        if (isRtfContent(source)) {
            return plainTextToNewsHtml(readRtfAsPlainText(source));
        }
        return markdownToNewsHtml(source);
    }

    private String normalizeNewsSource(String raw) {
        String source = raw == null ? "" : raw.replace("\uFEFF", "").trim();
        return source;
    }

    private boolean isRtfContent(String source) {
        if (source == null) {
            return false;
        }
        String normalized = source.replace("\uFEFF", "").trim();
        return normalized.startsWith("{\\rtf");
    }

    private String readRtfAsPlainText(String rtf) {
        try {
            RTFEditorKit kit = new RTFEditorKit();
            Document document = kit.createDefaultDocument();
            kit.read(new ByteArrayInputStream(rtf.getBytes(StandardCharsets.UTF_8)), document, 0);
            return document.getText(0, document.getLength()).trim();
        } catch (Exception ex) {
            return rtf;
        }
    }

    private String markdownToNewsHtml(String markdown) {
        String[] lines = markdown.replace("\r\n", "\n").replace('\r', '\n').split("\n");
        StringBuilder body = new StringBuilder();
        boolean inList = false;
        for (String rawLine : lines) {
            String line = rawLine.stripTrailing();
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                if (inList) {
                    body.append("</ul>");
                    inList = false;
                }
                continue;
            }
            if (trimmed.startsWith("# ")) {
                if (inList) {
                    body.append("</ul>");
                    inList = false;
                }
                body.append("<h1>").append(applyMarkdownInline(trimmed.substring(2))).append("</h1>");
                continue;
            }
            if (trimmed.startsWith("## ")) {
                if (inList) {
                    body.append("</ul>");
                    inList = false;
                }
                body.append("<h2>").append(applyMarkdownInline(trimmed.substring(3))).append("</h2>");
                continue;
            }
            if (trimmed.startsWith("### ")) {
                if (inList) {
                    body.append("</ul>");
                    inList = false;
                }
                body.append("<h3>").append(applyMarkdownInline(trimmed.substring(4))).append("</h3>");
                continue;
            }
            if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                if (!inList) {
                    body.append("<ul>");
                    inList = true;
                }
                body.append("<li>").append(applyMarkdownInline(trimmed.substring(2))).append("</li>");
                continue;
            }
            if (inList) {
                body.append("</ul>");
                inList = false;
            }
            body.append("<p>").append(applyMarkdownInline(trimmed)).append("</p>");
        }
        if (inList) {
            body.append("</ul>");
        }
        return wrapNewsHtml(body.toString());
    }

    private String applyMarkdownInline(String text) {
        String html = escapeHtml(text);
        html = html.replaceAll("`([^`]+)`", "<code>$1</code>");
        html = html.replaceAll("\\*\\*([^*]+)\\*\\*", "<b>$1</b>");
        html = html.replaceAll("\\*([^*]+)\\*", "<i>$1</i>");
        html = html.replaceAll("\\[([^\\]]+)]\\((https?://[^)]+)\\)", "<a href=\"$2\">$1</a>");
        return html;
    }

    private String plainTextToNewsHtml(String text) {
        String[] blocks = text.replace("\r\n", "\n").replace('\r', '\n').split("\n\\s*\n");
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < blocks.length; i++) {
            String block = blocks[i].trim();
            if (block.isEmpty()) {
                continue;
            }
            String htmlBlock = escapeHtml(block).replace("\n", "<br>");
            if (i == 0) {
                body.append("<h1>").append(htmlBlock).append("</h1>");
            } else {
                body.append("<p>").append(htmlBlock).append("</p>");
            }
        }
        return wrapNewsHtml(body.toString());
    }

    private String buildNewsLoadingHtml() {
        return wrapNewsHtml("<h1>Загрузка новостей</h1><p>Получаем актуальную информацию для страницы \"Что нового\"...</p>");
    }

    private String buildNewsErrorHtml(String text) {
        return wrapNewsHtml("<h1>Новости недоступны</h1><p>" + escapeHtml(text) + "</p>");
    }

    private String wrapNewsHtml(String body) {
        return "<html><head><style type=\"text/css\">"
                + "body { background:#181818; color:#f0f0f0; font-family:sans-serif; padding:0 0 24px 0; margin:0; }"
                + "h1 { font-size:28px; margin:0 0 18px 0; }"
                + "h2 { font-size:22px; margin:26px 0 12px 0; }"
                + "h3 { font-size:18px; margin:22px 0 10px 0; }"
                + "p { font-size:15px; line-height:1.55; margin:0 0 14px 0; color:#d8d8d8; }"
                + "ul { margin:0 0 16px 22px; color:#d8d8d8; }"
                + "li { margin:0 0 8px 0; font-size:15px; line-height:1.45; }"
                + "a { color:#67d44c; text-decoration:none; }"
                + "code { background:#242424; color:#ffffff; padding:2px 6px; }"
                + "</style></head><body>" + body + "</body></html>";
    }

    private JLabel createSettingsTab(String text, boolean active, Runnable onClick) {
        JLabel label = new JLabel(text);
        label.setForeground(active ? Color.WHITE : new Color(202, 202, 202));
        label.setFont(new Font(Font.SANS_SERIF, active ? Font.BOLD : Font.PLAIN, 14));
        label.setBorder(active
                ? BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 3, 0, ACCENT),
                new EmptyBorder(0, 0, 10, 0))
                : new EmptyBorder(0, 0, 13, 0));
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                onClick.run();
            }
        });
        return label;
    }

    private void switchSettingsTab(SettingsTabView tabView) {
        if (tabView == null || currentSettingsTabView == tabView) {
            return;
        }
        currentSettingsTabView = tabView;
        rebuildCenterContent();
    }

    private JLabel createSettingsBlockTitle(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Color.WHITE);
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JPanel createSettingsInlineRow(String labelText, JComponent field) {
        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel label = new JLabel(labelText.toUpperCase(java.util.Locale.ROOT));
        label.setForeground(new Color(196, 196, 196));
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        field.setAlignmentX(Component.LEFT_ALIGNMENT);

        row.add(label);
        row.add(Box.createVerticalStrut(6));
        row.add(field);
        return row;
    }

    private JPanel createOfficialCheckRow(JCheckBox checkBox) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        styleCheckBox(checkBox);
        row.add(checkBox);
        Dimension preferred = new Dimension(520, 30);
        row.setPreferredSize(preferred);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, preferred.height));
        return row;
    }

    private JPanel createLauncherBackgroundInlineRow() {
        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel label = new JLabel("ФОН");
        label.setForeground(new Color(196, 196, 196));
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel inline = new JPanel(new BorderLayout(10, 0));
        inline.setOpaque(false);
        inline.setAlignmentX(Component.LEFT_ALIGNMENT);
        inline.setMaximumSize(new Dimension(714, 52));

        inline.add(createOfficialField(launcherBackgroundPathField, 560), BorderLayout.CENTER);

        browseBackgroundButton.setPreferredSize(new Dimension(144, 52));
        browseBackgroundButton.setMinimumSize(new Dimension(144, 52));
        browseBackgroundButton.setMaximumSize(new Dimension(144, 52));
        inline.add(browseBackgroundButton, BorderLayout.EAST);

        row.add(label);
        row.add(Box.createVerticalStrut(6));
        row.add(inline);
        return row;
    }

    private JComponent createSettingsLauncherHint(String text) {
        JTextArea hint = new JTextArea(text);
        hint.setEditable(false);
        hint.setOpaque(false);
        hint.setLineWrap(true);
        hint.setWrapStyleWord(true);
        hint.setForeground(new Color(176, 176, 176));
        hint.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        hint.setBorder(null);
        hint.setMaximumSize(new Dimension(520, 44));
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        return hint;
    }

    private JComponent createOfficialField(JComponent component, int width) {
        if (component instanceof JTextField || component instanceof JFormattedTextField || component instanceof JPasswordField) {
            component.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(new Color(79, 168, 56), 1, false),
                    new EmptyBorder(10, 14, 10, 14)
            ));
        }
        if (component instanceof JComboBox<?> comboBox) {
            comboBox.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(new Color(79, 168, 56), 1, false),
                    new EmptyBorder(6, 10, 6, 10)
            ));
        }
        component.setMaximumSize(new Dimension(width, 52));
        component.setPreferredSize(new Dimension(width, 52));
        component.setMinimumSize(new Dimension(width, 52));
        return component;
    }

    private void styleSettingsScrollPane(JScrollPane scrollPane) {
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(12, 0));
        scrollPane.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                thumbColor = new Color(92, 92, 92);
                trackColor = new Color(24, 24, 24);
            }

            @Override
            protected JButton createDecreaseButton(int orientation) {
                return createZeroButton();
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                return createZeroButton();
            }

            private JButton createZeroButton() {
                JButton button = new JButton();
                button.setPreferredSize(new Dimension(0, 0));
                button.setMinimumSize(new Dimension(0, 0));
                button.setMaximumSize(new Dimension(0, 0));
                return button;
            }

            @Override
            protected void paintTrack(Graphics graphics, JComponent component, Rectangle trackBounds) {
                Graphics2D g2 = (Graphics2D) graphics.create();
                g2.setColor(new Color(20, 20, 20));
                g2.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
                g2.dispose();
            }

            @Override
            protected void paintThumb(Graphics graphics, JComponent component, Rectangle thumbBounds) {
                if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) {
                    return;
                }
                Graphics2D g2 = (Graphics2D) graphics.create();
                g2.setColor(new Color(98, 98, 98));
                g2.fillRoundRect(thumbBounds.x + 2, thumbBounds.y + 2, thumbBounds.width - 4, thumbBounds.height - 4, 8, 8);
                g2.dispose();
            }
        });
    }

    private JPanel createClientFolderInlineRow() {
        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel label = new JLabel("ПАПКА КЛИЕНТА");
        label.setForeground(new Color(210, 210, 210));
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextArea pathArea = new JTextArea(getSelectedGameDir().toString());
        pathArea.setEditable(false);
        pathArea.setLineWrap(true);
        pathArea.setWrapStyleWord(true);
        pathArea.setForeground(Color.WHITE);
        pathArea.setBackground(INPUT_BG);
        pathArea.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(79, 168, 56), 1, false),
                new EmptyBorder(12, 12, 12, 12)
        ));
        pathArea.setMaximumSize(new Dimension(714, 76));
        pathArea.setPreferredSize(new Dimension(714, 76));
        pathArea.setMinimumSize(new Dimension(714, 76));
        pathArea.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton openFolderButton = new JButton("Открыть папку клиента");
        styleDialogButton(openFolderButton, new Color(66, 66, 66), new Color(130, 130, 130));
        openFolderButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        openFolderButton.addActionListener(event -> openSelectedClientFolder());

        row.add(label);
        row.add(Box.createVerticalStrut(6));
        row.add(pathArea);
        row.add(Box.createVerticalStrut(10));
        row.add(openFolderButton);
        return row;
    }

    private JPanel createClientFolderSection() {
        JPanel section = createCard(new BorderLayout(0, 12), new Color(63, 63, 63), new EmptyBorder(16, 16, 16, 16));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("Папка клиента");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));

        JLabel subtitleLabel = new JLabel("Папка установки выбранного профиля");
        subtitleLabel.setForeground(new Color(196, 196, 196));
        subtitleLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));

        JTextArea pathArea = new JTextArea(getSelectedGameDir().toString());
        pathArea.setEditable(false);
        pathArea.setLineWrap(true);
        pathArea.setWrapStyleWord(true);
        pathArea.setForeground(Color.WHITE);
        pathArea.setBackground(INPUT_BG);
        pathArea.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(12, 12, 12, 12)
        ));

        JButton openFolderButton = new JButton("Открыть папку клиента");
        styleDialogButton(openFolderButton, new Color(66, 66, 66), new Color(130, 130, 130));
        openFolderButton.addActionListener(event -> openSelectedClientFolder());

        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        body.add(pathArea, BorderLayout.CENTER);
        body.add(openFolderButton, BorderLayout.SOUTH);

        header.add(titleLabel);
        header.add(Box.createVerticalStrut(4));
        header.add(subtitleLabel);
        section.add(header, BorderLayout.NORTH);
        section.add(body, BorderLayout.CENTER);
        return section;
    }

    private JPanel createSettingsSection(String title, String subtitle, Object[][] rows) {
        JPanel section = createCard(new BorderLayout(0, 12), new Color(63, 63, 63), new EmptyBorder(16, 16, 16, 16));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));

        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setForeground(new Color(196, 196, 196));
        subtitleLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));

        header.add(titleLabel);
        header.add(Box.createVerticalStrut(4));
        header.add(subtitleLabel);
        section.add(header, BorderLayout.NORTH);

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        for (int index = 0; index < rows.length; index++) {
            form.add(createStackedField(rows[index][0].toString(), (Component) rows[index][1]));
            if (index < rows.length - 1) {
                form.add(Box.createVerticalStrut(12));
            }
        }

        section.add(form, BorderLayout.CENTER);
        return section;
    }

    private JPanel createStackedField(String label, Component field) {
        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));

        JLabel rowLabel = new JLabel(label);
        rowLabel.setForeground(Color.WHITE);
        rowLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        rowLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        if (field instanceof JComponent component) {
            component.setAlignmentX(Component.LEFT_ALIGNMENT);
            component.setMaximumSize(new Dimension(Integer.MAX_VALUE, component.getPreferredSize().height));
        }

        row.add(rowLabel);
        row.add(Box.createVerticalStrut(8));
        row.add(field);
        return row;
    }

    private JPanel createCard(LayoutManager layout, Color background, EmptyBorder padding) {
        JPanel card = new JPanel(layout);
        card.setOpaque(true);
        card.setBackground(background);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(255, 255, 255, 40), 1, true),
                padding
        ));
        return card;
    }

    private void addRow(JPanel form, GridBagConstraints gbc, int row, String label, Component field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        JLabel rowLabel = new JLabel(label);
        rowLabel.setForeground(Color.WHITE);
        rowLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        form.add(rowLabel, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        form.add(field, gbc);
    }

    private void styleField(JComponent field) {
        field.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        field.setForeground(TEXT_PRIMARY);
        field.setBackground(INPUT_BG);
        field.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(10, 12, 10, 12)
        ));
        field.setPreferredSize(new Dimension(320, 40));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
    }

    private void styleLaunchButton() {
        launchButton.setText("PLAY");
        launchButton.setFont(new Font(Font.MONOSPACED, Font.BOLD, 22));
        launchButton.setForeground(Color.WHITE);
        launchButton.setFocusPainted(false);
        launchButton.setOpaque(false);
        launchButton.setContentAreaFilled(false);
        launchButton.setBorderPainted(false);
        launchButton.setBorder(new EmptyBorder(0, 0, 0, 0));
        launchButton.setPreferredSize(new Dimension(332, 64));
        launchButton.setMinimumSize(new Dimension(332, 64));
        launchButton.setMaximumSize(new Dimension(332, 64));
    }

    private void styleAddClientButton() {
        styleSidebarActionTile(addClientButton);
    }

    private void styleSideActionButton(JButton button) {
        styleSidebarActionTile(button);
    }

    private void styleWideSidebarActionButton(JButton button) {
        styleSidebarActionTile(button);
        button.setFont(new Font(Font.MONOSPACED, Font.BOLD, 13));
        button.setPreferredSize(new Dimension(66, 40));
        button.setMinimumSize(new Dimension(66, 40));
        button.setMaximumSize(new Dimension(66, 40));
        button.setToolTipText("Синхронизировать моды через ModSync");
    }

    private void styleSidebarActionTile(JButton button) {
        button.setFont(new Font(Font.MONOSPACED, Font.BOLD, 18));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setContentAreaFilled(true);
        button.setOpaque(true);
        button.setBackground(new Color(57, 53, 53));
        button.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(118, 118, 118), 1, false),
                new EmptyBorder(8, 14, 8, 14)
        ));
        button.setPreferredSize(new Dimension(54, 40));
        button.setMinimumSize(new Dimension(54, 40));
        button.setMaximumSize(new Dimension(54, 40));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void styleSettingsButton() {
        styleToolbarButton(settingsButton);
    }

    private void styleToolbarButton(JButton button) {
        button.setFont(new Font(Font.MONOSPACED, Font.BOLD, 8));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setContentAreaFilled(true);
        button.setBackground(new Color(42, 38, 38));
        button.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(88, 88, 88), 1, false),
                new EmptyBorder(4, 7, 4, 7)
        ));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void styleDialogButton(JButton button, Color background, Color borderColor) {
        button.setFocusPainted(false);
        button.setBackground(background);
        button.setForeground(Color.WHITE);
        button.setFont(new Font(Font.MONOSPACED, Font.BOLD, 11));
        button.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(borderColor, 1, false),
                new EmptyBorder(7, 11, 7, 11)
        ));
    }

    private void styleModeToggle(JToggleButton button) {
        button.setFocusPainted(false);
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        button.setForeground(Color.WHITE);
        button.setBackground(new Color(66, 66, 66));
        button.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(130, 130, 130), 1, true),
                new EmptyBorder(10, 14, 10, 14)
        ));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void styleCheckBox(JCheckBox checkBox) {
        checkBox.setOpaque(false);
        checkBox.setForeground(Color.WHITE);
        checkBox.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        checkBox.setFocusPainted(false);
        checkBox.setBorder(new EmptyBorder(0, 0, 0, 0));
        checkBox.setIcon(new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(new Color(30, 30, 30));
                g2.fillRect(x, y, 16, 16);
                g2.setColor(new Color(71, 200, 62));
                g2.drawRect(x, y, 15, 15);

                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return 16;
            }

            @Override
            public int getIconHeight() {
                return 16;
            }
        });
        checkBox.setSelectedIcon(new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(new Color(71, 200, 62));
                g2.fillRect(x, y, 16, 16);
                g2.setColor(new Color(17, 17, 17));
                g2.drawRect(x, y, 15, 15);

                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setColor(Color.WHITE);
                g2.drawLine(x + 4, y + 8, x + 7, y + 11);
                g2.drawLine(x + 7, y + 11, x + 12, y + 5);

                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return 16;
            }

            @Override
            public int getIconHeight() {
                return 16;
            }
        });
        checkBox.setIconTextGap(10);
    }

    private void styleComboBox(JComboBox<String> comboBox) {
        comboBox.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        comboBox.setForeground(TEXT_PRIMARY);
        comboBox.setBackground(INPUT_BG);
        comboBox.setOpaque(true);
        comboBox.setFocusable(false);
        comboBox.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(6, 10, 6, 10)
        ));
        comboBox.setMaximumRowCount(8);
        comboBox.setPrototypeDisplayValue("Встроенная Java 25");
        comboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        comboBox.setPreferredSize(new Dimension(360, 44));
        comboBox.setUI(new BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton arrow = new BasicArrowButton(
                        SwingConstants.SOUTH,
                        new Color(49, 49, 49),
                        new Color(49, 49, 49),
                        Color.WHITE,
                        new Color(49, 49, 49)
                );
                arrow.setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(BORDER, 1, true),
                        new EmptyBorder(0, 2, 0, 2)
                ));
                arrow.setContentAreaFilled(true);
                arrow.setFocusPainted(false);
                return arrow;
            }

            @Override
            public void paintCurrentValueBackground(Graphics graphics, Rectangle bounds, boolean hasFocus) {
                graphics.setColor(INPUT_BG);
                graphics.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }
        });
        comboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(javax.swing.JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
                if (isSelected) {
                    setBackground(ACCENT_DARK);
                    setForeground(Color.WHITE);
                } else {
                    setBackground(INPUT_BG);
                    setForeground(TEXT_PRIMARY);
                }
                setBorder(new EmptyBorder(8, 10, 8, 10));
                return component;
            }
        });
    }

    private void installLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        installUiDefaults();
    }

    private void installUiDefaults() {
        FontUIResource sansRegular = new FontUIResource(Font.SANS_SERIF, Font.PLAIN, 14);
        FontUIResource sansBold = new FontUIResource(Font.SANS_SERIF, Font.BOLD, 14);
        FontUIResource monoBold = new FontUIResource(Font.MONOSPACED, Font.BOLD, 14);
        ColorUIResource panelBg = new ColorUIResource(APP_BG);
        ColorUIResource controlBg = new ColorUIResource(new Color(57, 57, 57));
        ColorUIResource inputBg = new ColorUIResource(INPUT_BG);
        ColorUIResource textPrimary = new ColorUIResource(TEXT_PRIMARY);
        ColorUIResource textMuted = new ColorUIResource(TEXT_MUTED);
        ColorUIResource borderColor = new ColorUIResource(BORDER);
        ColorUIResource accent = new ColorUIResource(ACCENT);
        ColorUIResource transparentFocus = new ColorUIResource(new Color(0, 0, 0, 0));

        UIManager.put("Panel.background", panelBg);
        UIManager.put("OptionPane.background", panelBg);
        UIManager.put("OptionPane.foreground", textPrimary);
        UIManager.put("OptionPane.messageForeground", textPrimary);
        UIManager.put("OptionPane.messageFont", sansRegular);
        UIManager.put("OptionPane.buttonFont", sansBold);

        UIManager.put("Label.foreground", textPrimary);
        UIManager.put("Label.font", sansRegular);

        UIManager.put("Button.font", monoBold);
        UIManager.put("Button.foreground", textPrimary);
        UIManager.put("Button.background", controlBg);
        UIManager.put("Button.select", accent);
        UIManager.put("Button.focus", transparentFocus);

        UIManager.put("ToggleButton.font", sansBold);
        UIManager.put("ToggleButton.foreground", textPrimary);
        UIManager.put("ToggleButton.background", controlBg);
        UIManager.put("ToggleButton.select", accent);
        UIManager.put("ToggleButton.focus", transparentFocus);

        UIManager.put("TextField.font", sansRegular);
        UIManager.put("TextField.foreground", textPrimary);
        UIManager.put("TextField.background", inputBg);
        UIManager.put("TextField.caretForeground", textPrimary);
        UIManager.put("TextField.inactiveForeground", textMuted);

        UIManager.put("FormattedTextField.font", sansRegular);
        UIManager.put("FormattedTextField.foreground", textPrimary);
        UIManager.put("FormattedTextField.background", inputBg);
        UIManager.put("FormattedTextField.caretForeground", textPrimary);

        UIManager.put("PasswordField.font", sansRegular);
        UIManager.put("PasswordField.foreground", textPrimary);
        UIManager.put("PasswordField.background", inputBg);
        UIManager.put("PasswordField.caretForeground", textPrimary);

        UIManager.put("TextArea.font", new FontUIResource(Font.MONOSPACED, Font.PLAIN, 12));
        UIManager.put("TextArea.foreground", textPrimary);
        UIManager.put("TextArea.background", new ColorUIResource(LOG_BG));
        UIManager.put("TextArea.caretForeground", textPrimary);

        UIManager.put("ScrollPane.background", panelBg);
        UIManager.put("Viewport.background", panelBg);

        UIManager.put("CheckBox.font", sansBold);
        UIManager.put("CheckBox.foreground", textPrimary);
        UIManager.put("CheckBox.background", panelBg);
        UIManager.put("CheckBox.focus", transparentFocus);

        UIManager.put("ProgressBar.font", sansBold);
        UIManager.put("ProgressBar.foreground", accent);
        UIManager.put("ProgressBar.background", new ColorUIResource(new Color(24, 45, 17)));
        UIManager.put("ProgressBar.selectionForeground", textPrimary);
        UIManager.put("ProgressBar.selectionBackground", textPrimary);

        UIManager.put("Separator.foreground", borderColor);
        UIManager.put("Separator.background", borderColor);
    }

    private void launchAsync() {
        launchButton.setEnabled(false);
        modSyncButton.setEnabled(false);
        log("Preparing selected client...");

        new Thread(() -> {
            try {
                LauncherConfig currentConfig = collectConfig();
                currentConfig.save(configPath);
                this.config = currentConfig;

                ClientProfile selectedProfile = currentConfig.getSelectedProfile();
                Path selectedGameDir = getSelectedGameDir();
                Files.createDirectories(selectedGameDir);

                log("Selected client: " + selectedProfile.title);
                log("Install directory: " + selectedGameDir);

                JavaRuntimeInstaller javaRuntimeInstaller = new JavaRuntimeInstaller(httpClient, this::log);
                javaRuntimeInstaller.ensureRuntimeForProfile(projectDir, currentConfig, selectedProfile);

                MinecraftLaunchService launchService = new MinecraftLaunchService(projectDir, selectedGameDir, selectedProfile, httpClient, this::log);
                if (!selectedProfile.isOfficialManaged() && !selectedProfile.bundleUrl.isBlank()) {
                    GoogleDriveInstaller installer = new GoogleDriveInstaller(httpClient, this::log);
                    if (installer.isBundleInstallRequired(selectedProfile.bundleUrl, launchService.getGameDir())) {
                        GoogleDriveInstaller.TransferControl transferControl = beginBundleTransferUi(selectedProfile.title);
                        try {
                            installer.installBundle(
                                    selectedProfile.bundleUrl,
                                    projectDir,
                                    launchService.getGameDir(),
                                    this::onBundleTransferProgress,
                                    transferControl
                            );
                        } finally {
                            finishBundleTransferUi();
                        }
                    } else {
                        log("Google Drive сборка уже установлена, повторная загрузка не требуется.");
                    }
                } else if (selectedProfile.isOfficialManaged()) {
                    OfficialClientInstaller installer = new OfficialClientInstaller(httpClient, this::log);
                    installer.installClient(selectedProfile, projectDir, selectedGameDir, currentConfig.javaPath);
                } else {
                    LocalClientInstaller installer = new LocalClientInstaller(this::log);
                    installer.migrateLegacyClientIfNeeded(selectedProfile, projectDir, selectedGameDir);
                    log("Using local files from selected client folder.");
                }

                ElySession session;
                ElyAuthenticator authenticator = new ElyAuthenticator(httpClient);
                if (currentConfig.offlineMode) {
                    String nickname = usernameField.getText().trim();
                    if (nickname.isBlank()) {
                        throw new IllegalStateException("Enter a nickname for offline mode.");
                    }
                    ElySession refreshed = tryRefreshSavedSession(currentConfig, authenticator, nickname);
                    if (refreshed != null) {
                        session = refreshed;
                        currentConfig.username = refreshed.username();
                        this.config = currentConfig;
                        currentConfig.save(configPath);
                        log("Launching with saved Ely.by session as " + session.username());
                    } else {
                        session = ElySession.offline(nickname);
                        log("Launching in offline mode as " + session.username() + " without Ely.by session");
                    }
                } else {
                    String username = usernameField.getText().trim();
                    char[] password = passwordField.getPassword();
                    String resolvedUsername = username.isBlank() ? safeTrim(currentConfig.savedProfileName) : username;
                    ElySession restored = tryRefreshSavedSession(currentConfig, authenticator, resolvedUsername);
                    if (restored != null) {
                        session = restored;
                        currentConfig.username = restored.username();
                        this.config = currentConfig;
                        currentConfig.save(configPath);
                        log("Restored saved Ely.by session as " + session.username());
                    } else {
                        if (resolvedUsername.isBlank()) {
                            throw new IllegalStateException("Enter your Ely.by username or e-mail.");
                        }
                        if (password == null || password.length == 0) {
                            throw new IllegalStateException("Enter your Ely.by password.");
                        }
                        log("Authenticating with Ely.by...");
                        session = authenticator.authenticate(resolvedUsername, password, currentConfig.savedClientToken);
                        saveAuthenticatedSession(currentConfig, session);
                        log("Authenticated as " + session.username());
                    }
                    if (username.isBlank() && !session.username().isBlank()) {
                        usernameField.setText(session.username());
                    }
                    checkAuthenticatedSkinAsync(session);
                }

                Process process = launchService.launch(session, currentConfig);
                passwordField.setText("");
                log("Minecraft process started.");
                applyLauncherBehaviorOnGameStart(currentConfig.onGameLaunchAction);

                Thread outputThread = new Thread(() -> streamProcessOutput(process), "minecraft-output");
                outputThread.setDaemon(true);
                outputThread.start();
            } catch (Exception ex) {
                log("Launch failed: " + ex.getMessage());
            } finally {
                SwingUtilities.invokeLater(() -> {
                    launchButton.setEnabled(true);
                    modSyncButton.setEnabled(true);
                });
            }
        }, "launcher-worker").start();
    }

    private void syncModSyncAsync() {
        modSyncButton.setEnabled(false);
        launchButton.setEnabled(false);
        ClientProfile profile = config.getSelectedProfile();
        Path gameDir = getSelectedGameDir();
        log("Starting ModSync for " + profile.title + "...");

        new Thread(() -> {
            try {
                if (safeTrim(profile.serverAddress).isBlank()) {
                    throw new IllegalStateException("Укажи адрес сервера в настройках клиента.");
                }
                ModSyncInstaller installer = new ModSyncInstaller(httpClient, this::log);
                ModSyncManifestFetcher manifestFetcher = new ModSyncManifestFetcher(httpClient, this::log);
                beginModSyncUi(profile.title);
                ModSyncManifestFetcher.LiveManifest liveManifest = manifestFetcher.fetch(profile.serverAddress);
                if (liveManifest.hiddenPort() > 0) {
                    log("ModSync hidden HTTP port: " + liveManifest.hiddenPort());
                } else {
                    log("ModSync hidden HTTP port not found in MOTD. Using fallback URLs.");
                }
                log("ModSync live manifest source: " + liveManifest.sourceUrl());
                ModSyncInstaller.SyncSummary summary = installer.syncFromManifestJson(gameDir, liveManifest.json(), this::onModSyncProgress);
                if (summary.downloadedFiles() == 0 && summary.deletedFiles() == 0) {
                    log("ModSync: все файлы уже актуальны.");
                } else {
                    log("ModSync: загружено " + summary.downloadedFiles()
                            + " файл(ов), пропущено " + summary.skippedFiles()
                            + ", удалено лишних " + summary.deletedFiles() + ".");
                }
                if (profile.autoConnectEnabled && !safeTrim(profile.serverAddress).isBlank()) {
                    log("ModSync completed. Waiting 5 seconds before auto-launch...");
                    for (int seconds = 5; seconds >= 1; seconds--) {
                        log("Auto-launch in " + seconds + "...");
                        Thread.sleep(1000L);
                    }
                    log("ModSync completed. Launching client with auto-connect...");
                    SwingUtilities.invokeLater(this::launchAsync);
                    return;
                }
            } catch (Exception ex) {
                log("ModSync failed: " + ex.getMessage());
            } finally {
                finishBundleTransferUi();
                SwingUtilities.invokeLater(() -> {
                    modSyncButton.setEnabled(true);
                    launchButton.setEnabled(true);
                });
            }
        }, "modsync-worker").start();
    }

    private void addClientProfile() {
        persistSelectedProfileFields();

        String[] versionOptions = {"1.20.1"};
        String version = promptChoiceDialog("Добавить клиент", "Выберите версию Minecraft", versionOptions, versionOptions[0]);
        if (version == null) {
            return;
        }

        String[] sourceOptions = {"Официальный", "Google Drive"};
        String source = promptChoiceDialog("Добавить клиент", "Выберите источник клиента", sourceOptions, sourceOptions[0]);
        if (source == null) {
            return;
        }

        boolean googleDriveSource = "Google Drive".equals(source);

        String[] loaderOptions = new String[]{"Vanilla", "Forge"};
        String loader = promptChoiceDialog(
                "Добавить клиент",
                googleDriveSource ? "Выберите тип кастомного клиента" : "Выберите тип клиента",
                loaderOptions,
                loaderOptions[1]
        );
        if (loader == null) {
            return;
        }

        String bundleUrl = "";
        if (googleDriveSource) {
            bundleUrl = promptTextDialog("Добавить клиент", "Ссылка Google Drive на ZIP-архив клиента", "");
            if (bundleUrl == null) {
                return;
            }
            bundleUrl = bundleUrl.trim();
            if (bundleUrl.isBlank()) {
                JOptionPane.showMessageDialog(frame, "Нужно вставить ссылку Google Drive.", "Добавить клиент", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        String defaultName;
        if (googleDriveSource) {
            defaultName = "Forge".equals(loader) ? "Кастомный Minecraft " + version + " Forge" : "Кастомный Minecraft " + version;
        } else {
            defaultName = "Forge".equals(loader) ? "Minecraft " + version + " Forge" : "Minecraft " + version;
        }

        String title = promptTextDialog("Добавить клиент", "Название клиента", defaultName);
        if (title == null) {
            return;
        }
        title = title.trim();
        if (title.isBlank()) {
            title = defaultName;
        }

        ClientProfile profile = new ClientProfile();
        profile.title = title;
        profile.id = makeUniqueSlug(title);
        profile.folderName = profile.id;
        profile.bundleUrl = bundleUrl;
        profile.minecraftVersion = version;
        profile.loaderType = "Forge".equals(loader) ? "forge" : "vanilla";
        profile.loaderVersion = "Forge".equals(loader) ? "47.4.18" : "";
        profile.installSource = googleDriveSource ? "bundle" : "official";
        profile.subtitle = buildProfileSubtitle(profile);

        config.profiles.add(profile);
        config.selectedProfileId = profile.id;
        try {
            Files.createDirectories(instancesDir.resolve(profile.folderName));
        } catch (IOException ex) {
            log("Could not create client directory: " + ex.getMessage());
        }
        loadSelectedProfileIntoFields();
        saveConfigQuietly();
        refreshProfileUi();
        log("Added client profile: " + profile.title + " -> " + instancesDir.resolve(profile.folderName));
        if (googleDriveSource) {
            installBundleProfileAsync(profile);
        } else {
            installOfficialProfileAsync(profile);
        }
    }

    private void renameSelectedProfile() {
        persistSelectedProfileFields();
        ClientProfile profile = config.getSelectedProfile();
        String renamed = promptTextDialog("Переименовать клиент", "Название клиента", profile.title);
        if (renamed == null) {
            return;
        }
        renamed = renamed.trim();
        if (renamed.isBlank()) {
            return;
        }
        profile.title = renamed;
        saveConfigQuietly();
        refreshProfileUi();
        log("Renamed client profile to: " + profile.title);
    }

    private void deleteSelectedProfile() {
        config.ensureDefaults();
        if (config.profiles.size() <= 1) {
            JOptionPane.showMessageDialog(frame, "You cannot delete the last client profile.", "Delete client", JOptionPane.WARNING_MESSAGE);
            return;
        }

        ClientProfile profile = config.getSelectedProfile();
        int confirm = JOptionPane.showConfirmDialog(
                frame,
                "Delete profile \"" + profile.title + "\" from the launcher?\nFiles in its folder will stay on disk.",
                "Delete client",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        config.profiles.removeIf(candidate -> candidate.id.equals(profile.id));
        config.selectedProfileId = config.profiles.get(0).id;
        saveConfigQuietly();
        loadSelectedProfileIntoFields();
        refreshProfileUi();
        log("Deleted client profile: " + profile.title);
    }

    private void openSettingsDialog() {
        setCurrentPageView(PageView.SETTINGS);
    }

    private void openAccountDialog() {
        currentSettingsTabView = SettingsTabView.ACCOUNT;
        setCurrentPageView(PageView.SETTINGS);
    }

    private JPanel createAccountSection() {
        JPanel section = new JPanel();
        section.setOpaque(false);
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.setMaximumSize(new Dimension(760, Integer.MAX_VALUE));

        JLabel subtitleLabel = new JLabel("Выбери режим входа и данные аккаунта");
        subtitleLabel.setForeground(new Color(196, 196, 196));
        subtitleLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.add(subtitleLabel);
        section.add(Box.createVerticalStrut(12));

        JPanel statusCard = createCard(new BorderLayout(0, 12), new Color(44, 44, 44), new EmptyBorder(16, 18, 16, 18));
        statusCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusCard.setMaximumSize(new Dimension(760, 132));

        JPanel statusStack = new JPanel();
        statusStack.setOpaque(false);
        statusStack.setLayout(new BoxLayout(statusStack, BoxLayout.Y_AXIS));

        JLabel statusTitle = new JLabel("СТАТУС ВХОДА");
        statusTitle.setForeground(new Color(210, 210, 210));
        statusTitle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        statusTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel statusHint = new JLabel("Сохраняй Ely.by-сессию и управляй ей прямо из лаунчера");
        statusHint.setForeground(new Color(180, 180, 180));
        statusHint.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        statusHint.setAlignmentX(Component.LEFT_ALIGNMENT);

        accountModeStatusLabel.setOpaque(true);
        accountModeStatusLabel.setForeground(Color.WHITE);
        accountModeStatusLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        accountModeStatusLabel.setBorder(new EmptyBorder(8, 12, 8, 12));
        accountModeStatusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        statusStack.add(statusTitle);
        statusStack.add(Box.createVerticalStrut(10));
        statusStack.add(accountModeStatusLabel);
        statusStack.add(Box.createVerticalStrut(10));
        statusStack.add(statusHint);
        statusCard.add(statusStack, BorderLayout.CENTER);

        JPanel fieldsCard = createCard(new BorderLayout(0, 0), new Color(44, 44, 44), new EmptyBorder(18, 18, 18, 18));
        fieldsCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        fieldsCard.setLayout(new BoxLayout(fieldsCard, BoxLayout.Y_AXIS));
        fieldsCard.setMaximumSize(new Dimension(760, 420));

        JPanel fieldsHeader = new JPanel();
        fieldsHeader.setOpaque(false);
        fieldsHeader.setLayout(new BoxLayout(fieldsHeader, BoxLayout.Y_AXIS));

        JLabel fieldsTitle = new JLabel("ДАННЫЕ АККАУНТА");
        fieldsTitle.setForeground(Color.WHITE);
        fieldsTitle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        fieldsTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel fieldsHint = new JLabel("Онлайн-вход использует Ely.by, оффлайн запускает клиент только по нику");
        fieldsHint.setForeground(new Color(180, 180, 180));
        fieldsHint.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        fieldsHint.setAlignmentX(Component.LEFT_ALIGNMENT);

        fieldsHeader.add(fieldsTitle);
        fieldsHeader.add(Box.createVerticalStrut(6));
        fieldsHeader.add(fieldsHint);

        rebuildAccountFields();
        fieldsCard.add(fieldsHeader);
        fieldsCard.add(Box.createVerticalStrut(16));
        fieldsCard.add(accountFieldsPanel);

        JPanel skinCard = createSkinCheckSection();
        skinCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        skinCard.setMaximumSize(new Dimension(760, 220));

        section.add(statusCard);
        section.add(Box.createVerticalStrut(16));
        section.add(fieldsCard);
        section.add(Box.createVerticalStrut(16));
        section.add(skinCard);
        section.add(Box.createVerticalGlue());
        return section;
    }

    private JPanel createSkinCheckSection() {
        JPanel section = createCard(new BorderLayout(0, 16), new Color(44, 44, 44), new EmptyBorder(18, 18, 18, 18));
        section.setOpaque(true);
        section.setBackground(new Color(44, 44, 44));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("ELY.BY ИНСТРУМЕНТЫ");
        title.setForeground(Color.WHITE);
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel hint = new JLabel("Проверка скина, профиль Ely.by и быстрые действия по аккаунту");
        hint.setForeground(new Color(180, 180, 180));
        hint.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);

        header.add(title);
        header.add(Box.createVerticalStrut(6));
        header.add(hint);

        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));

        styleSkinInfoLabel(skinCheckStatusLabel);
        styleSkinInfoLabel(skinCheckUuidLabel);
        styleSkinInfoLabel(skinCheckProfileLabel);

        skinCheckStatusLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));

        info.add(skinCheckStatusLabel);
        info.add(Box.createVerticalStrut(4));
        info.add(skinCheckProfileLabel);
        info.add(Box.createVerticalStrut(2));
        info.add(skinCheckUuidLabel);

        updateSkinCheckLabels("Ещё не проверялось", safeTrim(config.savedProfileName), safeTrim(config.savedProfileUuid));

        JPanel buttonWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        buttonWrap.setOpaque(false);
        buttonWrap.add(checkSkinButton);
        buttonWrap.add(Box.createHorizontalStrut(8));
        buttonWrap.add(openElyProfileButton);
        buttonWrap.add(Box.createHorizontalStrut(8));
        buttonWrap.add(copyUuidButton);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.add(info);
        body.add(Box.createVerticalStrut(14));
        body.add(buttonWrap);

        section.add(header, BorderLayout.NORTH);
        section.add(body, BorderLayout.CENTER);
        return section;
    }

    private JPanel createAuthModeSelector() {
        JPanel selector = new JPanel(new GridBagLayout());
        selector.setOpaque(false);
        selector.setMaximumSize(new Dimension(714, 44));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 0, 8);
        selector.add(onlineModeButton, gbc);

        gbc.gridx = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        selector.add(offlineModeButton, gbc);
        return selector;
    }

    private void rebuildAccountFields() {
        boolean offline = offlineModeButton.isSelected();
        boolean authenticated = !offline && hasSavedElySession();

        accountFieldsPanel.removeAll();
        accountFieldsPanel.setLayout(new BoxLayout(accountFieldsPanel, BoxLayout.Y_AXIS));
        accountFieldsPanel.setOpaque(false);
        accountFieldsPanel.setBackground(new Color(0, 0, 0, 0));
        accountFieldsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        accountFieldsPanel.setMaximumSize(new Dimension(714, Integer.MAX_VALUE));

        accountFieldsPanel.add(createSettingsInlineRow("Режим", createOfficialField(createAuthModeSelector(), 714)));

        if (offline) {
            accountFieldsPanel.add(Box.createVerticalStrut(14));
            accountFieldsPanel.add(createSettingsInlineRow("Ник", createOfficialField(usernameField, 714)));
        } else if (authenticated) {
            String profileName = safeTrim(config.savedProfileName);
            if (!profileName.isBlank()) {
                usernameField.setText(profileName);
            }
            accountFieldsPanel.add(Box.createVerticalStrut(14));
            accountFieldsPanel.add(createSettingsInlineRow("Пользователь Ely.by", createOfficialReadOnlyField(
                    profileName.isBlank() ? "Авторизованный аккаунт" : profileName,
                    714
            )));
            accountFieldsPanel.add(Box.createVerticalStrut(14));
            accountFieldsPanel.add(createSettingsInlineRow("Действие", createInlineButtonRow(accountLogoutButton, 714)));
        } else {
            accountFieldsPanel.add(Box.createVerticalStrut(14));
            accountFieldsPanel.add(createSettingsInlineRow("Логин Ely.by", createOfficialField(usernameField, 714)));
            accountFieldsPanel.add(Box.createVerticalStrut(14));
            accountFieldsPanel.add(createSettingsInlineRow("Пароль Ely.by", createOfficialField(passwordField, 714)));
            accountFieldsPanel.add(Box.createVerticalStrut(14));
            accountFieldsPanel.add(createSettingsInlineRow("Вход", createInlineButtonRow(accountLoginButton, 714)));
        }
        accountFieldsPanel.revalidate();
        accountFieldsPanel.repaint();
    }

    private JComponent createOfficialReadOnlyField(String value, int width) {
        JLabel label = new JLabel(value);
        label.setForeground(Color.WHITE);
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        label.setOpaque(true);
        label.setBackground(INPUT_BG);
        label.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(79, 168, 56), 1, false),
                new EmptyBorder(10, 14, 10, 14)
        ));
        label.setHorizontalAlignment(SwingConstants.LEFT);
        return createOfficialField(label, width);
    }

    private JComponent createInlineButtonRow(JButton button, int width) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(width, 38));
        row.setPreferredSize(new Dimension(width, 38));
        row.setMinimumSize(new Dimension(width, 38));
        row.add(button);
        return row;
    }

    private void styleSkinInfoLabel(JLabel label) {
        label.setForeground(new Color(232, 232, 232));
        label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
    }

    private void selectProfile(String profileId) {
        if (profileId == null || profileId.equals(config.selectedProfileId)) {
            return;
        }
        persistSelectedProfileFields();
        config.selectedProfileId = profileId;
        loadSelectedProfileIntoFields();
        refreshProfileUi();
        scheduleConfigSave();
        log("Selected client: " + config.getSelectedProfile().title);
    }

    private void refreshProfileUi() {
        rebuildClientList();
        ClientProfile profile = config.getSelectedProfile();
        selectedClientTileTitle.setText(profile.title);
        selectedClientTileSubtitle.setText(profile.subtitle);
        installSourceTileSubtitle.setText(profile.bundleUrl.isBlank()
                ? "Local files in launcher/instances/" + profile.folderName
                : "Google Drive bundle -> launcher/instances/" + profile.folderName);
        playClientNameLabel.setText(profile.title);
        playClientVersionLabel.setText(profile.subtitle);
        playClientHintLabel.setText(buildProfileHint(profile));
        deleteClientButton.setEnabled(config.profiles.size() > 1);
        refreshSkinPreviewAsync();
        refreshServerStatusAsync();
        rebuildCenterContent();
        if (frame != null) {
            frame.repaint();
        }
    }

    private void rebuildClientList() {
        clientListPanel.removeAll();
        config.ensureDefaults();
        ClientProfile selected = config.getSelectedProfile();
        for (ClientProfile profile : config.profiles) {
            clientListPanel.add(createSidebarEntry(profile, profile.id.equals(selected.id)));
        }
        clientListPanel.revalidate();
        clientListPanel.repaint();
    }

    private void loadGlobalConfigIntoFields() {
        setOfflineMode(config.offlineMode);
        usernameField.setText(config.username);
        launcherBackgroundPathField.setText(safeTrim(config.launcherBackgroundPath));
        javaPathField.setText(config.javaPath);
        launcherLogModeComboBox.setSelectedItem(toLauncherLogModeLabel(config.launcherLogMode));
        launcherOnGameLaunchComboBox.setSelectedItem(toLaunchActionLabel(config.onGameLaunchAction));
        minMemoryField.setValue(config.minMemoryMb);
        maxMemoryField.setValue(config.maxMemoryMb);
        widthField.setValue(config.width);
        heightField.setValue(config.height);
        fullscreenCheckBox.setSelected(config.fullscreen);
        refreshBannerFromConfig();
    }

    private void loadSelectedProfileIntoFields() {
        config.ensureDefaults();
        ClientProfile profile = config.getSelectedProfile();
        clientBundleUrlField.setText(profile.bundleUrl);
        javaRuntimeComboBox.setSelectedItem(toJavaRuntimeLabel(profile.javaRuntime));
        minecraftArgumentsField.setText(safeTrim(profile.minecraftArguments));
        launchWrapperCommandField.setText(safeTrim(profile.launchWrapperCommand));
        serverAddressField.setText(safeTrim(profile.serverAddress));
        autoConnectCheckBox.setSelected(profile.autoConnectEnabled);
    }

    private void persistSelectedProfileFields() {
        config.ensureDefaults();
        ClientProfile profile = config.getSelectedProfile();
        profile.bundleUrl = clientBundleUrlField.getText().trim();
        profile.javaRuntime = fromJavaRuntimeLabel((String) javaRuntimeComboBox.getSelectedItem());
        profile.minecraftArguments = minecraftArgumentsField.getText().trim();
        profile.launchWrapperCommand = launchWrapperCommandField.getText().trim();
        profile.serverAddress = serverAddressField.getText().trim();
        profile.autoConnectEnabled = autoConnectCheckBox.isSelected();
    }

    private LauncherConfig collectConfig() {
        config.ensureDefaults();
        persistSelectedProfileFields();
        config.username = usernameField.getText().trim();
        config.offlineMode = offlineModeButton.isSelected();
        config.launcherBackgroundPath = launcherBackgroundPathField.getText().trim();
        config.launcherLogMode = fromLauncherLogModeLabel((String) launcherLogModeComboBox.getSelectedItem());
        config.onGameLaunchAction = fromLaunchActionLabel((String) launcherOnGameLaunchComboBox.getSelectedItem());
        config.javaPath = javaPathField.getText().trim();
        config.minMemoryMb = parseInt(minMemoryField.getText(), 2048);
        config.maxMemoryMb = parseInt(maxMemoryField.getText(), 4096);
        config.width = parseInt(widthField.getText(), 1280);
        config.height = parseInt(heightField.getText(), 720);
        config.fullscreen = fullscreenCheckBox.isSelected();
        refreshBannerFromConfig();
        return config;
    }

    private void saveConfigQuietly() {
        try {
            collectConfig().save(configPath);
        } catch (IOException ex) {
            log("Could not save launcher config: " + ex.getMessage());
        }
    }

    private void scheduleConfigSave() {
        if (profileSaveTimer != null && profileSaveTimer.isRunning()) {
            profileSaveTimer.restart();
            return;
        }

        profileSaveTimer = new Timer(180, event -> saveConfigQuietly());
        profileSaveTimer.setRepeats(false);
        profileSaveTimer.start();
    }

    private Path getSelectedGameDir() {
        ClientProfile profile = config.getSelectedProfile();
        String folderName = (profile.folderName == null || profile.folderName.isBlank()) ? profile.id : profile.folderName;
        return instancesDir.resolve(folderName);
    }

    private void chooseLauncherBackground() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Выберите фон лаунчера");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setAcceptAllFileFilterUsed(true);
        int result = chooser.showOpenDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION && chooser.getSelectedFile() != null) {
            launcherBackgroundPathField.setText(chooser.getSelectedFile().getAbsolutePath());
            refreshBannerFromConfig();
            scheduleConfigSave();
        }
    }

    private void refreshBannerFromConfig() {
        String path = launcherBackgroundPathField.getText();
        currentBannerImage = resolveConfiguredBannerImage(path);
        if (centerContentHost != null) {
            centerContentHost.repaint();
        }
    }

    private void refreshServerStatusAsync() {
        ClientProfile profile = config.getSelectedProfile();
        String address = safeTrim(profile.serverAddress);
        serverStatusRequestKey = address + "|" + System.nanoTime();
        String requestKey = serverStatusRequestKey;

        if (address.isBlank()) {
            serverIconImage = null;
            serverNameLabel.setText("Сервер не указан");
            serverPlayersLabel.setText("Игроки: -");
            serverPingLabel.setText("Пинг: -");
            serverAddressLabel.setText("Адрес: не задан");
            if (frame != null) {
                frame.repaint();
            }
            return;
        }

        serverIconImage = null;
        serverNameLabel.setText("Проверяем сервер...");
        serverPlayersLabel.setText("Игроки: -");
        serverPingLabel.setText("Пинг: ...");
        serverAddressLabel.setText(address);
        if (frame != null) {
            frame.repaint();
        }

        new Thread(() -> {
            try {
                MinecraftServerStatusFetcher fetcher = new MinecraftServerStatusFetcher();
                MinecraftServerStatusFetcher.ServerStatus status = fetcher.fetch(address, 2500, 3500);
                SwingUtilities.invokeLater(() -> {
                    if (!requestKey.equals(serverStatusRequestKey)) {
                        return;
                    }
                    serverIconImage = status.icon();
                    serverNameLabel.setText(status.displayName());
                    serverPlayersLabel.setText("Игроки: " + status.onlinePlayers() + " / " + status.maxPlayers());
                    serverPingLabel.setText("Пинг: " + status.pingMillis() + " мс");
                    serverAddressLabel.setText(address);
                    if (frame != null) {
                        frame.repaint();
                    }
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    if (!requestKey.equals(serverStatusRequestKey)) {
                        return;
                    }
                    serverIconImage = null;
                    serverNameLabel.setText("Сервер недоступен");
                    serverPlayersLabel.setText("Игроки: -");
                    serverPingLabel.setText("Пинг: ошибка");
                    serverAddressLabel.setText(address);
                    if (frame != null) {
                        frame.repaint();
                    }
                });
            }
        }, "server-status-worker").start();
    }

    private void openServerAddress() {
        String address = safeTrim(config.getSelectedProfile().serverAddress);
        if (address.isBlank()) {
            log("Server address is not configured for this client.");
            return;
        }
        try {
            String normalized = address.contains("://") ? address : "http://" + address;
            Desktop.getDesktop().browse(java.net.URI.create(normalized));
        } catch (Exception ex) {
            log("Could not open server address: " + ex.getMessage());
        }
    }

    private BufferedImage resolveConfiguredBannerImage(String customPath) {
        String candidate = safeTrim(customPath);
        if (!candidate.isBlank()) {
            try {
                Path path = Path.of(candidate);
                if (Files.isRegularFile(path)) {
                    BufferedImage image = ImageIO.read(path.toFile());
                    if (image != null) {
                        return image;
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return bannerImage;
    }

    private BufferedImage getCurrentBannerImage() {
        return currentBannerImage != null ? currentBannerImage : bannerImage;
    }

    private String toJavaRuntimeLabel(String runtime) {
        if (runtime == null) {
            return "Авто";
        }
        return switch (runtime.toLowerCase()) {
            case "java17" -> "Встроенная Java 17";
            case "java21" -> "Встроенная Java 21";
            case "java25" -> "Встроенная Java 25";
            case "custom" -> "Пользовательский путь";
            default -> "Авто";
        };
    }

    private String fromJavaRuntimeLabel(String label) {
        if (label == null) {
            return "auto";
        }
        return switch (label) {
            case "Встроенная Java 17" -> "java17";
            case "Встроенная Java 21" -> "java21";
            case "Встроенная Java 25" -> "java25";
            case "Пользовательский путь" -> "custom";
            default -> "auto";
        };
    }

    private String toLauncherLogModeLabel(String runtime) {
        return "file".equalsIgnoreCase(safeTrim(runtime)) ? "Сохранять в файл" : "Отключён";
    }

    private String fromLauncherLogModeLabel(String label) {
        return "Сохранять в файл".equalsIgnoreCase(safeTrim(label)) ? "file" : "disabled";
    }

    private String toLaunchActionLabel(String action) {
        return switch (safeTrim(action).toLowerCase(Locale.ROOT)) {
            case "hide" -> "Скрывать лаунчер";
            case "close" -> "Закрывать лаунчер";
            default -> "Ничего не делать";
        };
    }

    private String fromLaunchActionLabel(String label) {
        String normalized = safeTrim(label).toLowerCase(Locale.ROOT);
        if (normalized.contains("скрывать")) {
            return "hide";
        }
        if (normalized.contains("закрывать")) {
            return "close";
        }
        return "nothing";
    }

    private void openSelectedClientFolder() {
        try {
            Path selectedGameDir = getSelectedGameDir();
            Files.createDirectories(selectedGameDir);
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(selectedGameDir.toFile());
            } else {
                log("Desktop open is not supported on this system.");
            }
        } catch (Exception ex) {
            log("Could not open client folder: " + ex.getMessage());
        }
    }

    private void updateAuthModeUi() {
        boolean offline = offlineModeButton.isSelected();
        boolean authenticated = !offline && hasSavedElySession();
        onlineModeButton.setSelected(!offline);
        offlineModeButton.setSelected(offline);
        onlineModeButton.setBackground(offline ? new Color(66, 66, 66) : ACCENT_DARK);
        offlineModeButton.setBackground(offline ? ACCENT_DARK : new Color(66, 66, 66));
        passwordField.setEnabled(!offline && !authenticated);
        passwordField.setEditable(!offline && !authenticated);
        usernameField.setEditable(!authenticated);
        if (offline) {
            passwordField.setText("");
            passwordField.setToolTipText("Для оффлайн-режима нужен только ник");
            usernameField.setToolTipText("Ник для запуска и загрузки скинов Ely.by");
            accountModeStatusLabel.setText("Текущий режим: оффлайн-вход только по нику");
            accountModeStatusLabel.setBackground(new Color(121, 88, 33));
        } else if (authenticated) {
            passwordField.setText("");
            passwordField.setToolTipText("Для активной Ely.by-сессии пароль не нужен");
            usernameField.setToolTipText("Активный Ely.by аккаунт");
            accountModeStatusLabel.setText("Текущий режим: Ely.by-сессия активна");
            accountModeStatusLabel.setBackground(new Color(45, 116, 31));
        } else {
            passwordField.setToolTipText("Пароль аккаунта Ely.by");
            usernameField.setToolTipText("Логин Ely.by");
            accountModeStatusLabel.setText("Текущий режим: онлайн-вход в аккаунт Ely.by");
            accountModeStatusLabel.setBackground(new Color(45, 116, 31));
        }
        rebuildAccountFields();
        refreshAccountSettingsView();
    }

    private void setOfflineMode(boolean offline) {
        offlineModeButton.setSelected(offline);
        onlineModeButton.setSelected(!offline);
        updateAuthModeUi();
    }

    private void toggleLogs() {
        boolean show = !logContainer.isVisible();
        logContainer.setVisible(show);
        logToggleButton.setText(show ? "Скрыть логи" : "Показать логи");
        if (frame != null) {
            frame.revalidate();
            frame.repaint();
        }
    }

    private void logoutCurrentAccount() {
        LauncherConfig currentConfig = collectConfig();
        String fallbackName = safeTrim(currentConfig.savedProfileName);
        clearSavedSession(currentConfig);
        currentConfig.offlineMode = true;
        if (!fallbackName.isBlank()) {
            currentConfig.username = fallbackName;
        }
        setOfflineMode(true);
        try {
            currentConfig.save(configPath);
        } catch (IOException ex) {
            log("Could not save launcher config: " + ex.getMessage());
        }
        this.config = currentConfig;
        refreshProfileUi();
        log("Signed out from Ely.by session.");
    }

    private void authenticateAccountAsync() {
        if (offlineModeButton.isSelected()) {
            log("Для Ely.by входа переключитесь в онлайн-режим.");
            return;
        }

        accountLoginButton.setEnabled(false);
        new Thread(() -> {
            try {
                LauncherConfig currentConfig = collectConfig();
                String username = safeTrim(usernameField.getText());
                char[] password = passwordField.getPassword();

                if (username.isBlank()) {
                    throw new IllegalStateException("Enter your Ely.by username or e-mail.");
                }
                if (password == null || password.length == 0) {
                    throw new IllegalStateException("Enter your Ely.by password.");
                }

                log("Authenticating with Ely.by...");
                ElyAuthenticator authenticator = new ElyAuthenticator(httpClient);
                ElySession session = authenticator.authenticate(username, password, currentConfig.savedClientToken);
                saveAuthenticatedSession(currentConfig, session);
                passwordField.setText("");
                log("Authenticated as " + session.username());
                checkAuthenticatedSkinAsync(session);
            } catch (Exception ex) {
                log("Authentication failed: " + ex.getMessage());
            } finally {
                SwingUtilities.invokeLater(() -> accountLoginButton.setEnabled(true));
            }
        }, "account-auth-worker").start();
    }

    private boolean hasSavedElySession() {
        return !safeTrim(config.savedAccessToken).isBlank()
                && !safeTrim(config.savedClientToken).isBlank()
                && !safeTrim(config.savedProfileName).isBlank();
    }

    private void refreshAccountSettingsView() {
        if (accountFieldsPanel.getParent() != null) {
            accountFieldsPanel.revalidate();
            accountFieldsPanel.repaint();
        }
        if (currentPageView == PageView.SETTINGS && currentSettingsTabView == SettingsTabView.ACCOUNT && centerContentHost != null) {
            centerContentHost.revalidate();
            centerContentHost.repaint();
        }
    }

    private void openElyAccountManagement() {
        try {
            if (!Desktop.isDesktopSupported()) {
                log("Desktop browse is not supported on this system.");
                return;
            }
            Desktop.getDesktop().browse(java.net.URI.create("https://account.ely.by/"));
        } catch (Exception ex) {
            log("Could not open Ely.by account management: " + ex.getMessage());
        }
    }

    private ElySession tryRefreshSavedSession(LauncherConfig currentConfig, ElyAuthenticator authenticator, String nickname) {
        try {
            if (currentConfig.savedAccessToken == null || currentConfig.savedAccessToken.isBlank()) {
                return null;
            }
            if (currentConfig.savedClientToken == null || currentConfig.savedClientToken.isBlank()) {
                return null;
            }
            if (currentConfig.savedProfileName == null || currentConfig.savedProfileName.isBlank()) {
                return null;
            }
            if (!currentConfig.savedProfileName.equalsIgnoreCase(nickname.trim())) {
                return null;
            }

            log("Refreshing saved Ely.by session...");
            ElySession session = authenticator.refresh(currentConfig.savedAccessToken, currentConfig.savedClientToken);
            saveAuthenticatedSession(currentConfig, session);
            return session;
        } catch (Exception ex) {
            log("Saved Ely.by session expired or invalid, using plain offline mode.");
            clearSavedSession(currentConfig);
            try {
                currentConfig.save(configPath);
            } catch (IOException ignored) {
            }
            return null;
        }
    }

    private void saveAuthenticatedSession(LauncherConfig currentConfig, ElySession session) throws IOException {
        currentConfig.savedAccessToken = session.accessToken();
        currentConfig.savedClientToken = session.clientToken();
        currentConfig.savedProfileUuid = session.uuid();
        currentConfig.savedProfileName = session.username();
        currentConfig.username = session.username();
        currentConfig.offlineMode = false;
        currentConfig.save(configPath);
        this.config = currentConfig;
        setOfflineMode(false);
        refreshAccountSettingsView();
        refreshSkinPreviewAsync();
    }

    private void clearSavedSession(LauncherConfig currentConfig) {
        currentConfig.savedAccessToken = "";
        currentConfig.savedClientToken = "";
        currentConfig.savedProfileUuid = "";
        currentConfig.savedProfileName = "";
        refreshAccountSettingsView();
        refreshSkinPreviewAsync();
        updateSkinCheckLabels("No saved Ely.by session", "", "");
    }

    private void checkElySkinAsync() {
        LauncherConfig currentConfig = collectConfig();
        String profileName = safeTrim(currentConfig.savedProfileName);
        String profileUuid = safeTrim(currentConfig.savedProfileUuid).replace("-", "");

        if (profileName.isBlank() || profileUuid.isBlank()) {
            updateSkinCheckLabels("No saved Ely.by session", profileName, profileUuid);
            return;
        }

        updateSkinCheckLabels("Checking Ely.by...", profileName, profileUuid);
        checkSkinButton.setEnabled(false);

        Thread worker = new Thread(() -> {
            String status;
            try {
                status = describeSkinStatus(profileUuid);
            } catch (Exception ex) {
                status = "Check failed: " + ex.getMessage();
            }

            String finalStatus = status;
            SwingUtilities.invokeLater(() -> {
                updateSkinCheckLabels(finalStatus, profileName, profileUuid);
                checkSkinButton.setEnabled(true);
            });
        }, "ely-skin-check");
        worker.setDaemon(true);
        worker.start();
    }

    private void checkAuthenticatedSkinAsync(ElySession session) {
        String profileName = safeTrim(session.username());
        String profileUuid = safeTrim(session.uuid()).replace("-", "");
        updateSkinCheckLabels("Checking Ely.by after login...", profileName, profileUuid);

        Thread worker = new Thread(() -> {
            String status;
            try {
                status = describeSkinStatus(profileUuid);
                log("Ely skin check for " + profileName + ": " + status);
            } catch (Exception ex) {
                status = "Check failed: " + ex.getMessage();
                log("Ely skin check failed for " + profileName + ": " + ex.getMessage());
            }

            String finalStatus = status;
            SwingUtilities.invokeLater(() -> updateSkinCheckLabels(finalStatus, profileName, profileUuid));
        }, "ely-skin-auto-check");
        worker.setDaemon(true);
        worker.start();
    }

    private String describeSkinStatus(String profileUuid) throws IOException, InterruptedException {
        JsonObject profileJson = fetchSkinProfile(profileUuid);
        String texturesPayload = extractTexturesPayload(profileJson);
        if (texturesPayload == null || texturesPayload.isBlank()) {
            return "Профиль найден, но свойства textures отсутствуют";
        }

        String decoded = new String(Base64.getDecoder().decode(texturesPayload), StandardCharsets.UTF_8);
        JsonObject texturesJson = GSON.fromJson(decoded, JsonObject.class);
        JsonObject textures = texturesJson != null && texturesJson.has("textures")
                ? texturesJson.getAsJsonObject("textures")
                : null;
        if (textures == null || textures.size() == 0) {
            return "payload textures пустой";
        }

        boolean hasSkin = textures.has("SKIN");
        boolean hasCape = textures.has("CAPE");
        if (hasSkin && hasCape) {
            return "В Ely.by session/profile есть SKIN и CAPE";
        }
        if (hasSkin) {
            return "В Ely.by session/profile есть SKIN";
        }
        if (hasCape) {
            return "Есть CAPE, но нет SKIN";
        }
        return "textures получены, но SKIN отсутствует";
    }

    private void updateSkinCheckLabels(String status, String profileName, String profileUuid) {
        skinCheckStatusLabel.setText("Статус: " + (status == null || status.isBlank() ? "-" : status));
        skinCheckProfileLabel.setText("Профиль: " + ((profileName == null || profileName.isBlank()) ? "-" : profileName));
        skinCheckUuidLabel.setText("UUID: " + ((profileUuid == null || profileUuid.isBlank()) ? "-" : profileUuid));
        boolean hasProfile = profileName != null && !profileName.isBlank();
        boolean hasUuid = profileUuid != null && !profileUuid.isBlank();
        openElyProfileButton.setEnabled(hasProfile);
        copyUuidButton.setEnabled(hasUuid);
        if (accountDialog != null) {
            accountDialog.revalidate();
            accountDialog.repaint();
        }
    }

    private void openSavedElyProfile() {
        String profileName = safeTrim(config.savedProfileName);
        if (profileName.isBlank()) {
            log("No saved Ely.by profile to open.");
            return;
        }
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(java.net.URI.create("https://ely.by/skins?search=" + profileName));
            } else {
                log("Desktop browse is not supported on this system.");
            }
        } catch (Exception ex) {
            log("Could not open Ely.by profile: " + ex.getMessage());
        }
    }

    private void copySavedUuid() {
        String uuid = safeTrim(config.savedProfileUuid);
        if (uuid.isBlank()) {
            log("No saved Ely.by UUID to copy.");
            return;
        }
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(uuid), null);
            log("Copied Ely.by UUID: " + uuid);
        } catch (Exception ex) {
            log("Could not copy UUID: " + ex.getMessage());
        }
    }

    private JsonObject fetchSkinProfile(String uuid) throws IOException, InterruptedException {
        HttpRequest profileRequest = HttpRequest.newBuilder()
                .uri(java.net.URI.create("https://authserver.ely.by/session/profile/" + uuid))
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();
        HttpResponse<String> profileResponse = httpClient.send(profileRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (profileResponse.statusCode() / 100 != 2) {
            throw new IOException("HTTP " + profileResponse.statusCode());
        }
        JsonObject profileJson = GSON.fromJson(profileResponse.body(), JsonObject.class);
        if (profileJson == null) {
            throw new IOException("Empty response");
        }
        return profileJson;
    }

    private void refreshSkinPreviewAsync() {
        LauncherConfig currentConfig = this.config;
        String profileName = safeTrim(currentConfig.savedProfileName);
        String profileUuid = safeTrim(currentConfig.savedProfileUuid).replace("-", "");
        String requestKey = profileUuid + "|" + profileName;
        skinPreviewRequestKey = requestKey;

        if (profileUuid.isBlank() || profileName.isBlank()) {
            SwingUtilities.invokeLater(() -> {
                accountSkinHeadImage = null;
                accountAvatarPanel.repaint();
                skinPreviewPanel.showIdle(currentConfig.offlineMode
                        ? "Offline mode"
                        : "No Ely.by session");
            });
            return;
        }

        SwingUtilities.invokeLater(() -> skinPreviewPanel.showLoading(profileName));

        Thread worker = new Thread(() -> {
            try {
                BufferedImage skin = fetchSkinTexture(profileUuid);
                if (!requestKey.equals(skinPreviewRequestKey)) {
                    return;
                }
                SwingUtilities.invokeLater(() -> {
                    accountSkinHeadImage = extractSkinHead(skin);
                    accountAvatarPanel.repaint();
                    skinPreviewPanel.showSkin(profileName, profileUuid, skin);
                });
            } catch (Exception ex) {
                if (!requestKey.equals(skinPreviewRequestKey)) {
                    return;
                }
                SwingUtilities.invokeLater(() -> {
                    accountSkinHeadImage = null;
                    accountAvatarPanel.repaint();
                    skinPreviewPanel.showUnavailable(profileName, "Skin unavailable");
                });
            }
        }, "ely-skin-preview");
        worker.setDaemon(true);
        worker.start();
    }

    private BufferedImage extractSkinHead(BufferedImage skin) {
        if (skin == null || skin.getWidth() < 32 || skin.getHeight() < 16) {
            return null;
        }
        int scale = 8;
        BufferedImage head = new BufferedImage(8 * scale, 8 * scale, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = head.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2.drawImage(skin, 0, 0, 8 * scale, 8 * scale, 8, 8, 16, 16, null);
        if (skin.getWidth() >= 64 && skin.getHeight() >= 32) {
            g2.drawImage(skin, 0, 0, 8 * scale, 8 * scale, 40, 8, 48, 16, null);
        }
        g2.dispose();
        return head;
    }

    private BufferedImage fetchSkinTexture(String uuid) throws IOException, InterruptedException {
        JsonObject profileJson = fetchSkinProfile(uuid);
        String texturesPayload = extractTexturesPayload(profileJson);
        if (texturesPayload == null || texturesPayload.isBlank()) {
            return null;
        }

        String decoded = new String(Base64.getDecoder().decode(texturesPayload), StandardCharsets.UTF_8);
        JsonObject texturesJson = GSON.fromJson(decoded, JsonObject.class);
        if (texturesJson == null || !texturesJson.has("textures")) {
            return null;
        }

        JsonObject textures = texturesJson.getAsJsonObject("textures");
        if (textures == null || !textures.has("SKIN")) {
            return null;
        }

        JsonObject skinObject = textures.getAsJsonObject("SKIN");
        if (skinObject == null || !skinObject.has("url")) {
            return null;
        }

        String url = skinObject.get("url").getAsString();
        if (url == null || url.isBlank()) {
            return null;
        }

        HttpRequest imageRequest = HttpRequest.newBuilder()
                .uri(java.net.URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();
        HttpResponse<InputStream> imageResponse = httpClient.send(imageRequest, HttpResponse.BodyHandlers.ofInputStream());
        if (imageResponse.statusCode() / 100 != 2) {
            throw new IOException("Skin image HTTP " + imageResponse.statusCode());
        }

        try (InputStream inputStream = imageResponse.body()) {
            return ImageIO.read(inputStream);
        }
    }

    private String extractTexturesPayload(JsonObject profileJson) {
        if (!profileJson.has("properties")) {
            return null;
        }
        JsonArray properties = profileJson.getAsJsonArray("properties");
        if (properties == null) {
            return null;
        }
        for (JsonElement element : properties) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject property = element.getAsJsonObject();
            if (!property.has("name") || !property.has("value")) {
                continue;
            }
            if ("textures".equals(property.get("name").getAsString())) {
                return property.get("value").getAsString();
            }
        }
        return null;
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private String makeUniqueSlug(String rawTitle) {
        String base = slugify(rawTitle);
        Set<String> used = new HashSet<>();
        for (ClientProfile profile : config.profiles) {
            used.add(profile.id);
            used.add(profile.folderName);
        }
        String candidate = base;
        int index = 2;
        while (used.contains(candidate)) {
            candidate = base + "-" + index;
            index++;
        }
        return candidate;
    }

    private String slugify(String value) {
        String slug = value.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return slug.isBlank() ? "client" : slug;
    }

    private String escapeHtml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private String buildProfileSubtitle(ClientProfile profile) {
        if ("custom".equalsIgnoreCase(profile.loaderType)) {
            return profile.minecraftVersion + " (Google Drive)";
        }
        if (profile.isForge()) {
            return profile.minecraftVersion + "-forge-" + profile.loaderVersion;
        }
        return profile.minecraftVersion;
    }

    private String buildProfileHint(ClientProfile profile) {
        if (profile.isOfficialManaged()) {
            return "Official " + (profile.isForge() ? "Forge" : "Minecraft") + " install in launcher/instances/" + profile.folderName;
        }
        if (profile.bundleUrl != null && !profile.bundleUrl.isBlank()) {
            return "Google Drive сборка в launcher/instances/" + profile.folderName;
        }
        return "Installs into launcher/instances/" + profile.folderName + " and launches with Ely.by";
    }

    private String promptChoiceDialog(String dialogTitle, String labelText, String[] options, String selectedOption) {
        JComboBox<String> comboBox = new JComboBox<>(options);
        styleComboBox(comboBox);
        comboBox.setSelectedItem(selectedOption);
        createOfficialField(comboBox, 300);

        JPanel panel = createDialogFormPanel(labelText, comboBox);
        return showStyledInputDialog(dialogTitle, panel, () -> (String) comboBox.getSelectedItem());
    }

    private String promptTextDialog(String dialogTitle, String labelText, String defaultValue) {
        JTextField textField = new JTextField(defaultValue == null ? "" : defaultValue);
        styleField(textField);
        createOfficialField(textField, 420);
        JPanel panel = createDialogFormPanel(labelText, textField);
        return showStyledInputDialog(dialogTitle, panel, () -> textField.getText());
    }

    private JPanel createDialogFormPanel(String labelText, JComponent input) {
        JPanel panel = new JPanel();
        panel.setBackground(APP_BG);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(18, 18, 10, 18));

        JLabel label = new JLabel(labelText);
        label.setForeground(TEXT_PRIMARY);
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel inputWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        inputWrap.setOpaque(false);
        inputWrap.setAlignmentX(Component.LEFT_ALIGNMENT);
        inputWrap.add(input);

        panel.add(label);
        panel.add(Box.createVerticalStrut(16));
        panel.add(inputWrap);
        panel.setPreferredSize(new Dimension(380, 110));
        return panel;
    }

    private String showStyledInputDialog(String dialogTitle, JPanel content, ValueSupplier<String> onAccept) {
        JDialog dialog = new JDialog(frame, dialogTitle, true);
        dialog.getContentPane().setBackground(APP_BG);
        dialog.setLayout(new BorderLayout());

        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(APP_BG);
        body.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(255, 255, 255, 40), 1, true),
                new EmptyBorder(12, 12, 12, 12)
        ));
        body.add(content, BorderLayout.CENTER);

        JButton okButton = new JButton("ОК");
        styleDialogButton(okButton, ACCENT, ACCENT_DARK);
        JButton cancelButton = new JButton("Отмена");
        styleDialogButton(cancelButton, new Color(66, 66, 66), new Color(130, 130, 130));

        final String[] result = {null};
        okButton.addActionListener(event -> {
            result[0] = onAccept.get();
            dialog.dispose();
        });
        cancelButton.addActionListener(event -> dialog.dispose());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        actions.setBorder(new EmptyBorder(0, 0, 12, 12));
        actions.add(cancelButton);
        actions.add(okButton);

        dialog.add(body, BorderLayout.CENTER);
        dialog.add(actions, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setMinimumSize(new Dimension(440, 210));
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
        return result[0];
    }

    private void installOfficialProfileAsync(ClientProfile profile) {
        new Thread(() -> {
            try {
                LauncherConfig currentConfig = collectConfig();
                OfficialClientInstaller installer = new OfficialClientInstaller(httpClient, this::log);
                Path gameDir = instancesDir.resolve(profile.folderName);
                installer.installClient(profile, projectDir, gameDir, currentConfig.javaPath);
                log("Official client installed: " + profile.title);
            } catch (Exception ex) {
                log("Official install failed: " + ex.getMessage());
            }
        }, "official-client-install").start();
    }

    private void installBundleProfileAsync(ClientProfile profile) {
        new Thread(() -> {
            try {
                if (profile.bundleUrl == null || profile.bundleUrl.isBlank()) {
                    throw new IllegalStateException("Для кастомного клиента нужна ссылка Google Drive.");
                }
                GoogleDriveInstaller installer = new GoogleDriveInstaller(httpClient, this::log);
                Path gameDir = instancesDir.resolve(profile.folderName);
                GoogleDriveInstaller.TransferControl transferControl = beginBundleTransferUi(profile.title);
                try {
                    installer.installBundle(
                            profile.bundleUrl,
                            projectDir,
                            gameDir,
                            this::onBundleTransferProgress,
                            transferControl
                    );
                } finally {
                    finishBundleTransferUi();
                }
                log("Google Drive клиент установлен: " + profile.title);
            } catch (Exception ex) {
                log("Установка Google Drive клиента не удалась: " + ex.getMessage());
            }
        }, "bundle-client-install").start();
    }

    private GoogleDriveInstaller.TransferControl beginBundleTransferUi(String title) {
        GoogleDriveInstaller.TransferControl transferControl = new GoogleDriveInstaller.TransferControl();
        activeBundleTransferControl = transferControl;
        SwingUtilities.invokeLater(() -> {
            bundleTransferStatusLabel.setText("Загрузка сборки: " + title);
            bundleTransferDetailLabel.setText("Подготовка к загрузке...");
            bundleTransferProgressBar.setIndeterminate(true);
            bundleTransferProgressBar.setValue(0);
            bundleTransferProgressBar.setString("Подготовка...");
            bundlePauseButton.setText("Пауза");
            bundlePauseButton.setEnabled(true);
            bundleCancelButton.setEnabled(true);
            bundleTransferPanel.setVisible(true);
            bundleTransferPanel.revalidate();
            bundleTransferPanel.repaint();
        });
        return transferControl;
    }

    private void beginModSyncUi(String title) {
        activeBundleTransferControl = null;
        SwingUtilities.invokeLater(() -> {
            bundleTransferStatusLabel.setText("ModSync: " + title);
            bundleTransferDetailLabel.setText("Подготовка к синхронизации...");
            bundleTransferProgressBar.setIndeterminate(true);
            bundleTransferProgressBar.setValue(0);
            bundleTransferProgressBar.setString("Подготовка...");
            bundlePauseButton.setEnabled(false);
            bundleCancelButton.setEnabled(false);
            bundleTransferPanel.setVisible(true);
            bundleTransferPanel.revalidate();
            bundleTransferPanel.repaint();
        });
    }

    private void onBundleTransferProgress(GoogleDriveInstaller.ProgressInfo info) {
        SwingUtilities.invokeLater(() -> {
            bundleTransferPanel.setVisible(true);
            bundleTransferStatusLabel.setText("extract".equals(info.stage()) ? "Установка сборки" : "Скачивание сборки");
            bundleTransferDetailLabel.setText(buildBundleTransferDetail(info));

            boolean indeterminate = info.indeterminate() || info.totalBytes() <= 0;
            bundleTransferProgressBar.setIndeterminate(indeterminate);
            if (indeterminate) {
                bundleTransferProgressBar.setString(info.message());
            } else {
                int percent = (int) Math.max(0, Math.min(100, (info.completedBytes() * 100L) / Math.max(1L, info.totalBytes())));
                bundleTransferProgressBar.setValue(percent);
                bundleTransferProgressBar.setString(percent + "%");
            }

            GoogleDriveInstaller.TransferControl transferControl = activeBundleTransferControl;
            bundlePauseButton.setText(transferControl != null && transferControl.isPaused() ? "Продолжить" : "Пауза");
        });
    }

    private void onModSyncProgress(ModSyncInstaller.ProgressInfo info) {
        SwingUtilities.invokeLater(() -> {
            bundleTransferPanel.setVisible(true);
            bundleTransferStatusLabel.setText("Синхронизация ModSync");
            bundleTransferDetailLabel.setText(buildModSyncTransferDetail(info));

            boolean indeterminate = info.indeterminate() || info.totalBytes() <= 0;
            bundleTransferProgressBar.setIndeterminate(indeterminate);
            if (indeterminate) {
                String prefix = info.totalFiles() > 0
                        ? info.completedFiles() + " / " + info.totalFiles() + " файлов"
                        : "Подготовка";
                bundleTransferProgressBar.setString(prefix);
            } else {
                int percent = (int) Math.max(0, Math.min(100, (info.completedBytes() * 100L) / Math.max(1L, info.totalBytes())));
                bundleTransferProgressBar.setValue(percent);
                bundleTransferProgressBar.setString(percent + "%");
            }
        });
    }

    private String buildBundleTransferDetail(GoogleDriveInstaller.ProgressInfo info) {
        if (info.totalBytes() > 0) {
            return info.message() + "  " + formatBytes(info.completedBytes()) + " / " + formatBytes(info.totalBytes());
        }
        if (info.completedBytes() > 0) {
            return info.message() + "  " + formatBytes(info.completedBytes());
        }
        return info.message();
    }

    private String buildModSyncTransferDetail(ModSyncInstaller.ProgressInfo info) {
        String fileLabel = safeTrim(info.currentFile()).isBlank() ? "ModSync" : info.currentFile();
        String progress = info.totalFiles() > 0
                ? info.completedFiles() + " / " + info.totalFiles() + " файлов"
                : "Подготовка";
        if (info.totalBytes() > 0) {
            return progress + "  " + formatBytes(info.completedBytes()) + " / " + formatBytes(info.totalBytes()) + "  " + fileLabel;
        }
        return progress + "  " + fileLabel;
    }

    private void toggleBundleTransferPause() {
        GoogleDriveInstaller.TransferControl transferControl = activeBundleTransferControl;
        if (transferControl == null) {
            return;
        }
        if (transferControl.isPaused()) {
            transferControl.resume();
            bundlePauseButton.setText("Пауза");
            bundleTransferDetailLabel.setText("Загрузка продолжена");
        } else {
            transferControl.pause();
            bundlePauseButton.setText("Продолжить");
            bundleTransferDetailLabel.setText("Загрузка поставлена на паузу");
        }
    }

    private void cancelBundleTransfer() {
        GoogleDriveInstaller.TransferControl transferControl = activeBundleTransferControl;
        if (transferControl == null) {
            return;
        }
        transferControl.cancel();
        bundlePauseButton.setEnabled(false);
        bundleCancelButton.setEnabled(false);
        bundleTransferProgressBar.setIndeterminate(false);
        bundleTransferProgressBar.setString("Отменено");
        bundleTransferDetailLabel.setText("Загрузка сборки отменяется...");
    }

    private void finishBundleTransferUi() {
        activeBundleTransferControl = null;
        SwingUtilities.invokeLater(this::setBundleTransferIdle);
    }

    private void setBundleTransferIdle() {
        bundleTransferPanel.setVisible(false);
        bundleTransferStatusLabel.setText("Загрузка сборки не активна");
        bundleTransferDetailLabel.setText(" ");
        bundleTransferProgressBar.setIndeterminate(false);
        bundleTransferProgressBar.setValue(0);
        bundleTransferProgressBar.setString("");
        bundlePauseButton.setText("Пауза");
        bundlePauseButton.setEnabled(false);
        bundleCancelButton.setEnabled(false);
    }

    private void streamProcessOutput(Process process) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log(line);
            }
            log("Minecraft exited with code " + process.waitFor());
        } catch (Exception ex) {
            log("Process log reader stopped: " + ex.getMessage());
        } finally {
            restoreLauncherAfterGameIfNeeded();
        }
    }

    private int parseInt(String value, int fallback) {
        try {
            String normalized = value == null ? "" : value.replaceAll("[^0-9-]", "");
            if (normalized.isBlank() || "-".equals(normalized)) {
                return fallback;
            }
            return Integer.parseInt(normalized);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024L) {
            return bytes + " B";
        }
        double value = bytes;
        String[] units = {"KB", "MB", "GB"};
        int unitIndex = -1;
        while (value >= 1024 && unitIndex < units.length - 1) {
            value /= 1024.0;
            unitIndex++;
        }
        return String.format("%.1f %s", value, units[Math.max(0, unitIndex)]);
    }

    private BufferedImage loadImageResource(String resourcePath, Path fallbackPath) {
        try (InputStream resourceStream = LauncherApp.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (resourceStream != null) {
                BufferedImage image = ImageIO.read(resourceStream);
                if (image != null) {
                    return image;
                }
            }
        } catch (IOException ignored) {
        }

        if (!Files.exists(fallbackPath)) {
            return null;
        }
        try {
            return ImageIO.read(fallbackPath.toFile());
        } catch (IOException ignored) {
            return null;
        }
    }

    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + System.lineSeparator());
            logArea.setCaretPosition(logArea.getDocument().getLength());
            reflectOfficialTransferInUi(message);
        });
        writeLauncherLogIfEnabled(message);
    }

    private void writeLauncherLogIfEnabled(String message) {
        if (!"file".equalsIgnoreCase(safeTrim(config.launcherLogMode)) || message == null) {
            return;
        }
        try {
            Path logDir = projectDir.resolve("launcher").resolve("logs");
            Files.createDirectories(logDir);
            Path logFile = logDir.resolve("launcher.log");
            String line = "[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "] "
                    + message + System.lineSeparator();
            Files.writeString(logFile, line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
        }
    }

    private void reflectOfficialTransferInUi(String message) {
        if (activeBundleTransferControl != null || message == null || message.isBlank()) {
            return;
        }

        if (message.startsWith("Downloading official metadata for Minecraft ")) {
            showOfficialTransferStatus("Подготовка Minecraft", message, true, 0);
            return;
        }
        if (message.startsWith("Preparing official Minecraft ")) {
            showOfficialTransferStatus("Подготовка Minecraft", message, true, 0);
            return;
        }
        if (message.startsWith("Downloading asset index ")) {
            showOfficialTransferStatus("Загрузка ресурсов", message, true, 0);
            return;
        }
        if (message.startsWith("Downloading official Forge installer")) {
            showOfficialTransferStatus("Загрузка Forge", message, true, 0);
            return;
        }
        if (message.startsWith("Installing official Forge ")) {
            showOfficialTransferStatus("Установка Forge", message, true, 0);
            return;
        }
        if (message.startsWith("Downloading official Ely.by Authlib patch")) {
            showOfficialTransferStatus("Подготовка Ely.by", message, true, 0);
            return;
        }
        if (message.startsWith("Downloading bundled Java ")) {
            showOfficialTransferStatus("Загрузка Java", message, true, 0);
            return;
        }
        if (message.startsWith("Extracting bundled Java runtime")) {
            showOfficialTransferStatus("Установка Java", message, true, 0);
            return;
        }
        if (message.startsWith("Bundled Java ")) {
            showOfficialTransferStatus("Java готова", message, false, 100);
            hideOfficialTransferStatusLater();
            return;
        }
        if (message.startsWith("Assets ready: ")) {
            int slashIndex = message.indexOf('/');
            int colonIndex = message.indexOf(':');
            if (slashIndex > colonIndex && colonIndex >= 0) {
                try {
                    int completed = Integer.parseInt(message.substring(colonIndex + 1, slashIndex).trim());
                    int total = Integer.parseInt(message.substring(slashIndex + 1).trim());
                    int percent = total > 0 ? Math.max(0, Math.min(100, (completed * 100) / total)) : 0;
                    showOfficialTransferStatus("Загрузка ресурсов", "Ресурсы готовы: " + completed + " / " + total, false, percent);
                    if (completed >= total && total > 0) {
                        hideOfficialTransferStatusLater();
                    }
                } catch (NumberFormatException ignored) {
                }
            }
            return;
        }
        if (message.startsWith("Minecraft process started.")
                || message.startsWith("Official client installed:")
                || message.startsWith("Google Drive клиент установлен:")) {
            hideOfficialTransferStatusLater();
        }
    }

    private void showOfficialTransferStatus(String title, String detail, boolean indeterminate, int percent) {
        bundleTransferPanel.setVisible(true);
        bundleTransferStatusLabel.setText(title);
        bundleTransferDetailLabel.setText(detail);
        bundlePauseButton.setEnabled(false);
        bundleCancelButton.setEnabled(false);
        bundlePauseButton.setText("Пауза");
        bundleTransferProgressBar.setIndeterminate(indeterminate);
        if (indeterminate) {
            bundleTransferProgressBar.setString("Загрузка...");
        } else {
            bundleTransferProgressBar.setValue(percent);
            bundleTransferProgressBar.setString(percent + "%");
        }
    }

    private void hideOfficialTransferStatusLater() {
        Timer timer = new Timer(1200, event -> setBundleTransferIdle());
        timer.setRepeats(false);
        timer.start();
    }

    private void applyLauncherBehaviorOnGameStart(String action) {
        String normalized = safeTrim(action).toLowerCase(Locale.ROOT);
        if (frame == null) {
            return;
        }
        if ("hide".equals(normalized)) {
            launcherWindowHiddenForGame = true;
            SwingUtilities.invokeLater(() -> frame.setVisible(false));
            return;
        }
        if ("close".equals(normalized)) {
            SwingUtilities.invokeLater(() -> {
                if (frame != null) {
                    frame.dispose();
                }
            });
        }
    }

    private void restoreLauncherAfterGameIfNeeded() {
        if (!launcherWindowHiddenForGame || frame == null) {
            return;
        }
        launcherWindowHiddenForGame = false;
        SwingUtilities.invokeLater(() -> {
            frame.setVisible(true);
            frame.toFront();
            frame.repaint();
        });
    }

    private static final class SkinPreviewPanel extends JPanel {
        private String title = "ELY PROFILE";
        private String subtitle = "Sign in to load skin";
        private BufferedImage skin;
        private boolean loading;

        private SkinPreviewPanel() {
            setOpaque(true);
            setBackground(new Color(16, 16, 16, 176));
            setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(new Color(255, 255, 255, 6), 1, false),
                    new EmptyBorder(8, 8, 8, 8)
            ));
            setPreferredSize(new Dimension(156, 126));
        }

        private void showIdle(String message) {
            this.title = "ELY PROFILE";
            this.subtitle = message;
            this.skin = null;
            this.loading = false;
            repaint();
        }

        private void showLoading(String profileName) {
            this.title = profileName;
            this.subtitle = "Loading skin preview...";
            this.skin = null;
            this.loading = true;
            repaint();
        }

        private void showSkin(String profileName, String uuid, BufferedImage skin) {
            this.title = profileName;
            this.subtitle = skin != null
                    ? "Skin loaded from Ely.by"
                    : "No published skin on Ely.by";
            this.skin = skin;
            this.loading = false;
            setToolTipText(uuid);
            repaint();
        }

        private void showUnavailable(String profileName, String message) {
            this.title = profileName;
            this.subtitle = message;
            this.skin = null;
            this.loading = false;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

            g2.setPaint(new GradientPaint(0, 0, new Color(27, 27, 27, 235), 0, getHeight(), new Color(11, 11, 11, 235)));
            g2.fillRect(0, 0, getWidth(), getHeight());

            int canvasX = 24;
            int canvasY = 54;
            int canvasW = getWidth() - 48;
            int canvasH = 56;

            g2.setColor(new Color(255, 255, 255, 12));
            g2.fillRect(canvasX, canvasY, canvasW, canvasH);

            if (skin != null) {
                drawCharacter(g2, skin, canvasX, canvasY, canvasW, canvasH);
            } else {
                drawPlaceholderCharacter(g2, canvasX, canvasY, canvasW, canvasH);
            }

            g2.setColor(Color.WHITE);
            g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, 7));
            g2.drawString("ELY PROFILE", 16, 18);

            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
            drawWrappedText(g2, title, 16, 30, getWidth() - 32, 12);

            g2.setColor(new Color(210, 210, 210));
            g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 7));
            drawWrappedText(g2, subtitle, 16, 42, getWidth() - 32, 10);

            if (loading) {
                g2.setColor(new Color(120, 205, 98));
                int dotY = 18;
                for (int i = 0; i < 3; i++) {
                    g2.fillOval(getWidth() - 38 + i * 9, dotY - 6, 5, 5);
                }
            }
            g2.dispose();
        }

        private void drawWrappedText(Graphics2D g2, String text, int x, int y, int width, int lineHeight) {
            if (text == null) {
                return;
            }
            String[] words = text.split("\\s+");
            StringBuilder line = new StringBuilder();
            int drawY = y;
            for (String word : words) {
                String candidate = line.isEmpty() ? word : line + " " + word;
                if (g2.getFontMetrics().stringWidth(candidate) > width && !line.isEmpty()) {
                    g2.drawString(line.toString(), x, drawY);
                    drawY += lineHeight;
                    line = new StringBuilder(word);
                } else {
                    line = new StringBuilder(candidate);
                }
            }
            if (!line.isEmpty()) {
                g2.drawString(line.toString(), x, drawY);
            }
        }

        private void drawCharacter(Graphics2D g2, BufferedImage skin, int canvasX, int canvasY, int canvasW, int canvasH) {
            boolean modern = skin.getWidth() >= 64 && skin.getHeight() >= 64;
            int scale = Math.max(4, Math.min(canvasW / 16, canvasH / 32));
            int bodyWidth = 16 * scale;
            int bodyHeight = 32 * scale;
            int x = canvasX + (canvasW - bodyWidth) / 2;
            int y = canvasY + Math.max(8, (canvasH - bodyHeight) / 2);

            drawPart(g2, skin, 8, 8, 8, 8, x + 4 * scale, y, 8 * scale, 8 * scale);
            drawPart(g2, skin, 40, 8, 8, 8, x + 4 * scale, y, 8 * scale, 8 * scale);

            drawPart(g2, skin, 20, 20, 8, 12, x + 4 * scale, y + 8 * scale, 8 * scale, 12 * scale);
            drawPart(g2, skin, 20, 36, 8, 12, x + 4 * scale, y + 8 * scale, 8 * scale, 12 * scale);

            drawPart(g2, skin, 44, 20, 4, 12, x, y + 8 * scale, 4 * scale, 12 * scale);
            drawPart(g2, skin, 44, 36, 4, 12, x, y + 8 * scale, 4 * scale, 12 * scale);
            if (modern) {
                drawPart(g2, skin, 36, 52, 4, 12, x + 12 * scale, y + 8 * scale, 4 * scale, 12 * scale);
                drawPart(g2, skin, 52, 52, 4, 12, x + 12 * scale, y + 8 * scale, 4 * scale, 12 * scale);
            } else {
                drawPart(g2, skin, 44, 20, 4, 12, x + 12 * scale, y + 8 * scale, 4 * scale, 12 * scale);
                drawPart(g2, skin, 44, 36, 4, 12, x + 12 * scale, y + 8 * scale, 4 * scale, 12 * scale);
            }

            drawPart(g2, skin, 4, 20, 4, 12, x + 4 * scale, y + 20 * scale, 4 * scale, 12 * scale);
            drawPart(g2, skin, 4, 36, 4, 12, x + 4 * scale, y + 20 * scale, 4 * scale, 12 * scale);
            if (modern) {
                drawPart(g2, skin, 20, 52, 4, 12, x + 8 * scale, y + 20 * scale, 4 * scale, 12 * scale);
                drawPart(g2, skin, 4, 52, 4, 12, x + 8 * scale, y + 20 * scale, 4 * scale, 12 * scale);
            } else {
                drawPart(g2, skin, 4, 20, 4, 12, x + 8 * scale, y + 20 * scale, 4 * scale, 12 * scale);
                drawPart(g2, skin, 4, 36, 4, 12, x + 8 * scale, y + 20 * scale, 4 * scale, 12 * scale);
            }
        }

        private void drawPart(Graphics2D g2, BufferedImage skin, int sx, int sy, int sw, int sh, int dx, int dy, int dw, int dh) {
            if (sx + sw > skin.getWidth() || sy + sh > skin.getHeight()) {
                return;
            }
            g2.drawImage(skin, dx, dy, dx + dw, dy + dh, sx, sy, sx + sw, sy + sh, null);
        }

        private void drawPlaceholderCharacter(Graphics2D g2, int canvasX, int canvasY, int canvasW, int canvasH) {
            int centerX = canvasX + canvasW / 2;
            int topY = canvasY + 22;

            g2.setColor(new Color(120, 120, 120));
            g2.fillRoundRect(centerX - 26, topY, 52, 52, 12, 12);
            g2.fillRoundRect(centerX - 20, topY + 56, 40, 74, 12, 12);
            g2.fillRoundRect(centerX - 44, topY + 58, 16, 64, 12, 12);
            g2.fillRoundRect(centerX + 28, topY + 58, 16, 64, 12, 12);
            g2.fillRoundRect(centerX - 18, topY + 134, 16, 58, 12, 12);
            g2.fillRoundRect(centerX + 2, topY + 134, 16, 58, 12, 12);

            g2.setColor(new Color(170, 170, 170));
            g2.fillOval(centerX - 12, topY + 16, 8, 8);
            g2.fillOval(centerX + 4, topY + 16, 8, 8);
            g2.fillRoundRect(centerX - 8, topY + 32, 16, 5, 5, 5);
        }
    }

    @FunctionalInterface
    private interface ValueSupplier<T> {
        T get();
    }

    private static final class RootPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setPaint(new GradientPaint(0, 0, new Color(35, 35, 35), 0, getHeight(), APP_BG));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
        }
    }

    private static final class ArtworkPanel extends JPanel {
        private final java.util.function.Supplier<BufferedImage> imageSupplier;

        private ArtworkPanel(java.util.function.Supplier<BufferedImage> imageSupplier) {
            this.imageSupplier = imageSupplier;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            BufferedImage image = imageSupplier != null ? imageSupplier.get() : null;
            if (image != null) {
                double scale = Math.max(
                        getWidth() / (double) image.getWidth(),
                        getHeight() / (double) image.getHeight()
                );
                int drawWidth = (int) Math.ceil(image.getWidth() * scale);
                int drawHeight = (int) Math.ceil(image.getHeight() * scale);
                int drawX = (getWidth() - drawWidth) / 2;
                int drawY = (getHeight() - drawHeight) / 2;
                g2.drawImage(image, drawX, drawY, drawWidth, drawHeight, null);
            } else {
                g2.setPaint(new GradientPaint(0, 0, new Color(164, 217, 255), getWidth(), getHeight(), new Color(22, 95, 206)));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }

            g2.setColor(new Color(255, 255, 255, 18));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
        }
    }

    private static final class MinecraftLogoPanel extends JComponent {
        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            String top = "MINECRAFT";
            String bottom = "JAVA EDITION";
            Font topFont = new Font(Font.MONOSPACED, Font.BOLD, 44);
            Font bottomFont = new Font(Font.MONOSPACED, Font.BOLD, 24);

            drawOutlinedCentered(g2, top, topFont, 26, new Color(240, 240, 240), new Color(20, 20, 20));
            drawOutlinedCentered(g2, bottom, bottomFont, 76, new Color(240, 240, 240), new Color(20, 20, 20));
            g2.dispose();
        }

        private void drawOutlinedCentered(Graphics2D g2, String text, Font font, int baselineY, Color fill, Color stroke) {
            g2.setFont(font);
            Rectangle2D bounds = g2.getFontMetrics().getStringBounds(text, g2);
            int x = (int) ((getWidth() - bounds.getWidth()) / 2);
            g2.setColor(stroke);
            for (int dx = -3; dx <= 3; dx++) {
                for (int dy = -3; dy <= 3; dy++) {
                    if (dx == 0 && dy == 0) {
                        continue;
                    }
                    g2.drawString(text, x + dx, baselineY + dy);
                }
            }
            g2.setColor(new Color(170, 170, 170));
            g2.drawString(text, x + 2, baselineY + 2);
            g2.setColor(fill);
            g2.drawString(text, x, baselineY);
        }
    }

    private static final class LogoImagePanel extends JComponent {
        private final BufferedImage image;

        private LogoImagePanel(BufferedImage image) {
            this.image = image;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            int targetWidth = Math.max(1, getWidth() - 10);
            double ratio = (double) image.getHeight() / image.getWidth();
            int targetHeight = (int) (targetWidth * ratio);
            int x = (getWidth() - targetWidth) / 2;
            int y = 12;
            g2.drawImage(image, x, y, targetWidth, targetHeight, null);
            g2.dispose();
        }
    }

    private static final class LauncherButton extends JButton {
        private final BufferedImage backgroundImage;

        private LauncherButton(String text, BufferedImage backgroundImage) {
            super(text);
            this.backgroundImage = backgroundImage;
            setContentAreaFilled(false);
            setOpaque(false);
            getModel().addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent event) {
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            if (backgroundImage != null) {
                if (!isEnabled()) {
                    g2.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 0.55f));
                } else if (getModel().isPressed()) {
                    g2.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 0.88f));
                }
                g2.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), null);
            } else {
                Color fill = new Color(88, 166, 57);
                if (!isEnabled()) {
                    fill = new Color(78, 105, 69);
                } else if (getModel().isPressed()) {
                    fill = new Color(62, 121, 42);
                } else if (getModel().isRollover()) {
                    fill = new Color(99, 182, 66);
                }

                g2.setColor(new Color(14, 14, 14));
                g2.fillRect(0, 0, getWidth(), getHeight());

                g2.setColor(fill);
                g2.fillRect(4, 4, getWidth() - 8, getHeight() - 8);

                g2.setColor(new Color(255, 255, 255, 28));
                g2.fillRect(4, 4, getWidth() - 8, 4);
                g2.setColor(new Color(52, 103, 35));
                g2.fillRect(4, getHeight() - 8, getWidth() - 8, 4);
            }
            g2.dispose();
            super.paintComponent(graphics);
        }
    }

    private static final class SidebarIconPanel extends JPanel {
        private final Color base;
        private final boolean active;
        private final BufferedImage image;

        private SidebarIconPanel(Color base, boolean active, BufferedImage image) {
            this.base = base;
            this.active = active;
            this.image = image;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g2 = (Graphics2D) graphics.create();
            if (image != null) {
                int availableWidth = getWidth();
                int availableHeight = getHeight();
                double scale = Math.min(
                        (double) availableWidth / Math.max(1, image.getWidth()),
                        (double) availableHeight / Math.max(1, image.getHeight())
                );
                int drawWidth = Math.max(1, (int) Math.round(image.getWidth() * scale));
                int drawHeight = Math.max(1, (int) Math.round(image.getHeight() * scale));
                int drawX = (availableWidth - drawWidth) / 2;
                int drawY = (availableHeight - drawHeight) / 2;
                g2.drawImage(image, drawX, drawY, drawWidth, drawHeight, null);
            } else {
                g2.setColor(base);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(active ? new Color(130, 205, 107) : new Color(165, 140, 110));
                g2.fillRect(0, 0, getWidth(), getHeight() / 3);
                g2.setColor(new Color(255, 255, 255, 35));
                int cell = Math.max(4, getWidth() / 6);
                for (int y = 0; y < getHeight(); y += cell) {
                    for (int x = ((y / cell) % 2) * 2; x < getWidth(); x += cell * 2) {
                        g2.fillRect(x, y, cell, cell);
                    }
                }
            }
            g2.setColor(new Color(255, 255, 255, 20));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
        }
    }

    private static final class PromoPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g2 = (Graphics2D) graphics.create();
            int block = 26;
            int startX = getWidth() - 4 * block - 6;
            int startY = getHeight() - 3 * block - 6;
            Color dark = new Color(34, 34, 34);
            Color bright = new Color(96, 190, 56);

            int[][] pattern = {
                    {1, 0, 1, 1},
                    {1, 1, 1, 0},
                    {0, 1, 0, 0}
            };

            for (int y = 0; y < pattern.length; y++) {
                for (int x = 0; x < pattern[y].length; x++) {
                    g2.setColor(pattern[y][x] == 1 ? bright : dark);
                    g2.fillRect(startX + x * block, startY + y * block, block - 2, block - 2);
                }
            }
            g2.dispose();
        }
    }

    private static final class UserIcon implements Icon {
        private final int size;
        private final Color color;

        private UserIcon(int size, Color color) {
            this.size = size;
            this.color = color;
        }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            int head = Math.max(6, size / 2);
            g2.fillOval(x + (size - head) / 2, y, head, head);
            g2.drawRoundRect(x + 2, y + head - 1, size - 4, size - head, 8, 8);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }

    private static final class HomeSidebarIcon implements Icon {
        private final int size;
        private final Color color;

        private HomeSidebarIcon(int size, Color color) {
            this.size = size;
            this.color = color;
        }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.translate(x, y);
            int roofY = 6;
            g2.drawLine(3, roofY + 7, size / 2, roofY);
            g2.drawLine(size / 2, roofY, size - 3, roofY + 7);
            g2.drawRect(5, roofY + 7, size - 10, size - 12);
            g2.drawRect(size / 2 - 2, roofY + 13, 5, size - 18);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }

    private static final class NewsSidebarIcon implements Icon {
        private final int size;
        private final Color color;

        private NewsSidebarIcon(int size, Color color) {
            this.size = size;
            this.color = color;
        }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.translate(x, y);
            g2.drawRect(2, 3, size - 6, size - 6);
            g2.drawLine(6, 7, size - 8, 7);
            g2.drawLine(6, 11, size - 8, 11);
            g2.drawLine(6, 15, size - 11, 15);
            g2.fillRect(5, 6, 1, 10);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }

    private static final class SlidersSidebarIcon implements Icon {
        private final int size;
        private final Color color;

        private SlidersSidebarIcon(int size, Color color) {
            this.size = size;
            this.color = color;
        }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.translate(x, y);

            int[] knobOffsets = {6, 12, 8};
            for (int i = 0; i < 3; i++) {
                int yPos = 4 + i * 6;
                g2.drawLine(2, yPos, size - 2, yPos);
                g2.fillRect(knobOffsets[i], yPos - 2, 5, 5);
            }
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }

    private static final class GearIcon implements Icon {
        private final int size;
        private final Color color;

        private GearIcon(int size, Color color) {
            this.size = size;
            this.color = color;
        }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.translate(x, y);
            g2.setColor(color);

            int center = size / 2;
            int outer = Math.max(6, size / 2 - 1);
            int inner = Math.max(3, size / 5);
            for (int index = 0; index < 8; index++) {
                double angle = Math.toRadians(index * 45.0);
                int toothX = center + (int) ((outer - 2) * Math.cos(angle));
                int toothY = center + (int) ((outer - 2) * Math.sin(angle));
                g2.fillRoundRect(toothX - 2, toothY - 2, 4, 4, 2, 2);
            }
            g2.fillOval(center - outer / 2, center - outer / 2, outer, outer);
            g2.setColor(new Color(57, 57, 57));
            g2.fillOval(center - inner, center - inner, inner * 2, inner * 2);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }
}
