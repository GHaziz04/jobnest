package com.utils;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.function.Consumer;

/**
 * Version simplifiée de l'authentification par reconnaissance faciale
 * Utilise la comparaison d'histogrammes au lieu de LBPH
 */
public class FaceIDAuthenticator {

    private static final String FACES_DATA_DIR = "face_data/";
    private static final int FACE_SIZE = 200;
    private static final double SIMILARITY_THRESHOLD = 0.56; // 70% de similarité requise (moins stricte)

    private CascadeClassifier faceDetector;
    private VideoCapture currentCamera;

    static {
        try {
            nu.pattern.OpenCV.loadShared();
            System.out.println("✅ OpenCV chargé avec succès !");
        } catch (Exception e) {
            System.err.println("❌ Erreur chargement OpenCV");
            e.printStackTrace();
        }
    }

    public FaceIDAuthenticator() {
        // Initialiser le détecteur de visages
        faceDetector = new CascadeClassifier();

        try {
            // Copier le fichier Haar Cascade depuis les ressources
            InputStream is = getClass().getResourceAsStream("/haarcascades/haarcascade_frontalface_default.xml");
            if (is == null) {
                System.err.println("Fichier haarcascade_frontalface_default.xml introuvable dans les ressources");
                // Essayer de charger depuis un fichier local
                File cascadeFile = new File("haarcascade_frontalface_default.xml");
                if (cascadeFile.exists()) {
                    faceDetector.load(cascadeFile.getAbsolutePath());
                }
            } else {
                // Créer un fichier temporaire
                File tempFile = File.createTempFile("cascade", ".xml");
                tempFile.deleteOnExit();
                Files.copy(is, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                faceDetector.load(tempFile.getAbsolutePath());
                is.close();
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement du classificateur de visages");
            e.printStackTrace();
        }

        // Créer le dossier de données si nécessaire
        new File(FACES_DATA_DIR).mkdirs();
    }

    /**
     * Enregistrer un nouveau visage pour un utilisateur (version sans callback)
     */
    public boolean enrollFace(int userId, String email) {
        return enrollFace(userId, email, null);
    }

    /**
     * Enregistrer Face ID depuis une image de profil (photo de création de compte)
     */
    public static boolean enrollFaceFromImage(int userId, String imagePath) {
        System.out.println("🖼️ Enregistrement Face ID depuis l'image: " + imagePath);

        try {
            // Charger l'image
            Mat image = Imgcodecs.imread(imagePath);
            if (image.empty()) {
                System.err.println("❌ Impossible de charger l'image");
                return false;
            }

            // Convertir en niveaux de gris
            Mat grayImage = new Mat();
            if (image.channels() > 1) {
                Imgproc.cvtColor(image, grayImage, Imgproc.COLOR_BGR2GRAY);
            } else {
                grayImage = image.clone();
            }
            Imgproc.equalizeHist(grayImage, grayImage);

            // Créer le détecteur de visages
            CascadeClassifier faceDetector = new CascadeClassifier();
            InputStream is = FaceIDAuthenticator.class.getResourceAsStream("/haarcascades/haarcascade_frontalface_default.xml");

            if (is == null) {
                File cascadeFile = new File("haarcascade_frontalface_default.xml");
                if (cascadeFile.exists()) {
                    faceDetector.load(cascadeFile.getAbsolutePath());
                }
            } else {
                File tempFile = File.createTempFile("cascade", ".xml");
                tempFile.deleteOnExit();
                Files.copy(is, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                faceDetector.load(tempFile.getAbsolutePath());
                is.close();
            }

            // Détecter les visages dans l'image
            MatOfRect faceDetections = new MatOfRect();
            faceDetector.detectMultiScale(grayImage, faceDetections, 1.1, 5, 0,
                    new Size(100, 100), new Size());

            Rect[] faces = faceDetections.toArray();

            if (faces.length == 0) {
                System.err.println("❌ Aucun visage détecté dans l'image");
                image.release();
                grayImage.release();
                faceDetections.release();
                return false;
            }

            // Prendre le visage le plus grand
            Rect largestFace = faces[0];
            for (Rect rect : faces) {
                if (rect.width * rect.height > largestFace.width * largestFace.height) {
                    largestFace = rect;
                }
            }

            System.out.println("✅ Visage détecté: " + largestFace.width + "x" + largestFace.height);

            // Extraire le visage
            Mat face = new Mat(grayImage, largestFace);
            Mat resizedFace = new Mat();
            Imgproc.resize(face, resizedFace, new Size(FACE_SIZE, FACE_SIZE));

            // Créer plusieurs variations de l'image pour améliorer la reconnaissance
            List<Mat> faceVariations = new ArrayList<>();

            // Original
            faceVariations.add(resizedFace.clone());

            // Légèrement plus lumineux
            Mat brightFace = new Mat();
            resizedFace.convertTo(brightFace, -1, 1.1, 10);
            faceVariations.add(brightFace);

            // Légèrement plus sombre
            Mat darkFace = new Mat();
            resizedFace.convertTo(darkFace, -1, 0.9, -10);
            faceVariations.add(darkFace);

            // Avec un peu de flou (simule légère imperfection caméra)
            Mat blurredFace = new Mat();
            Imgproc.GaussianBlur(resizedFace, blurredFace, new Size(3, 3), 0);
            faceVariations.add(blurredFace);

            // Légèrement tourné à gauche
            Mat rotatedLeft = rotateFace(resizedFace, -5);
            if (!rotatedLeft.empty()) faceVariations.add(rotatedLeft);

            // Légèrement tourné à droite
            Mat rotatedRight = rotateFace(resizedFace, 5);
            if (!rotatedRight.empty()) faceVariations.add(rotatedRight);

            // Dupliquer les variations pour atteindre 20 échantillons
            while (faceVariations.size() < 20) {
                int idx = faceVariations.size() % 6;
                faceVariations.add(faceVariations.get(idx).clone());
            }

            System.out.println("📸 Généré " + faceVariations.size() + " variations du visage");

            // Sauvegarder les données faciales
            FaceIDAuthenticator authenticator = new FaceIDAuthenticator();
            authenticator.saveFaceDataPublic(userId, faceVariations);

            // Libérer les ressources
            image.release();
            grayImage.release();
            face.release();
            resizedFace.release();
            faceDetections.release();
            for (Mat variation : faceVariations) {
                variation.release();
            }

            System.out.println("✅ Face ID enregistré avec succès pour l'utilisateur " + userId);
            return true;

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'enregistrement Face ID depuis l'image");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Faire pivoter légèrement un visage (pour créer des variations)
     */
    private static Mat rotateFace(Mat face, double angleDegrees) {
        try {
            Point center = new Point(face.cols() / 2.0, face.rows() / 2.0);
            Mat rotationMatrix = Imgproc.getRotationMatrix2D(center, angleDegrees, 1.0);
            Mat rotated = new Mat();
            Imgproc.warpAffine(face, rotated, rotationMatrix, face.size());
            rotationMatrix.release();
            return rotated;
        } catch (Exception e) {
            return new Mat();
        }
    }

    /**
     * Enregistrer un nouveau visage pour un utilisateur (avec callback pour affichage)
     */
    public boolean enrollFace(int userId, String email, Consumer<Mat> frameCallback) {
        currentCamera = new VideoCapture(0);

        if (!currentCamera.isOpened()) {
            System.err.println("Impossible d'ouvrir la caméra");
            return false;
        }

        // Laisser la caméra s'initialiser
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        List<Mat> faces = new ArrayList<>();
        Mat frame = new Mat();
        Mat grayFrame = new Mat();
        Mat displayFrame = new Mat();
        int capturedFaces = 0;
        int requiredSamples = 20; // Augmenter à 20 échantillons pour meilleure précision

        System.out.println("Positionnez votre visage face à la caméra...");

        while (capturedFaces < requiredSamples && currentCamera != null) {
            boolean success = currentCamera.read(frame);

            if (success && !frame.empty()) {
                // Afficher la frame avec callback
                if (frameCallback != null) {
                    frame.copyTo(displayFrame);
                    frameCallback.accept(displayFrame);
                }

                Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);
                Imgproc.equalizeHist(grayFrame, grayFrame);

                MatOfRect faceDetections = new MatOfRect();
                faceDetector.detectMultiScale(grayFrame, faceDetections, 1.2, 4, 0,
                        new Size(120, 120), new Size(400, 400));

                Rect[] detectedFaces = faceDetections.toArray();

                if (detectedFaces.length > 0) {
                    // Prendre le visage le plus grand
                    Rect largestFace = detectedFaces[0];
                    for (Rect rect : detectedFaces) {
                        if (rect.width * rect.height > largestFace.width * largestFace.height) {
                            largestFace = rect;
                        }
                    }

                    Mat face = new Mat(grayFrame, largestFace);
                    Mat resizedFace = new Mat();
                    Imgproc.resize(face, resizedFace, new Size(FACE_SIZE, FACE_SIZE));

                    faces.add(resizedFace.clone());
                    capturedFaces++;

                    System.out.println("Échantillon capturé : " + capturedFaces + "/" + requiredSamples);

                    // Libérer
                    face.release();
                    resizedFace.release();

                    // Petit délai pour éviter des images trop similaires
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        break;
                    }
                }

                faceDetections.release();
            }

            try {
                Thread.sleep(30); // ~30 FPS
            } catch (InterruptedException e) {
                break;
            }
        }

        // Libérer toutes les ressources
        frame.release();
        grayFrame.release();
        displayFrame.release();
        stopCamera();

        // Sauvegarder les données du visage
        if (faces.size() >= requiredSamples) {
            saveFaceData(userId, faces);
            // Libérer les faces
            for (Mat face : faces) {
                face.release();
            }
            return true;
        }

        // Libérer les faces en cas d'échec
        for (Mat face : faces) {
            face.release();
        }
        return false;
    }

    /**
     * Authentifier un utilisateur via reconnaissance faciale (avec callback pour affichage)
     */
    public Integer authenticateWithFace(Consumer<Mat> frameCallback) {
        currentCamera = new VideoCapture(0);

        if (!currentCamera.isOpened()) {
            System.err.println("Impossible d'ouvrir la caméra");
            return null;
        }

        // Laisser la caméra s'initialiser
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Mat frame = new Mat();
        Mat grayFrame = new Mat();
        Mat displayFrame = new Mat();
        Mat resizedFace = new Mat();
        Integer authenticatedUserId = null;
        int attempts = 0;
        int maxAttempts = 100; // Réduire à 100 tentatives (environ 3 secondes)
        int detectionInterval = 3; // Détecter tous les 3 frames pour optimiser

        System.out.println("Positionnez votre visage face à la caméra...");

        while (attempts < maxAttempts && authenticatedUserId == null && currentCamera != null) {
            boolean success = currentCamera.read(frame);

            if (success && !frame.empty()) {
                // Cloner pour l'affichage
                frame.copyTo(displayFrame);

                // Dessiner un cadre de guidage au centre
                int centerX = displayFrame.cols() / 2;
                int centerY = displayFrame.rows() / 2;
                int rectWidth = 300;
                int rectHeight = 400;
                Point topLeft = new Point(centerX - rectWidth/2, centerY - rectHeight/2);
                Point bottomRight = new Point(centerX + rectWidth/2, centerY + rectHeight/2);
                Imgproc.rectangle(displayFrame, topLeft, bottomRight, new Scalar(212, 150, 109), 3);

                // Callback pour afficher la frame
                if (frameCallback != null) {
                    frameCallback.accept(displayFrame);
                }

                // Ne détecter que tous les N frames pour optimiser
                if (attempts % detectionInterval == 0) {
                    // Conversion en niveaux de gris pour la détection
                    Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);
                    Imgproc.equalizeHist(grayFrame, grayFrame);

                    // Détection des visages avec paramètres optimisés
                    MatOfRect faceDetections = new MatOfRect();
                    faceDetector.detectMultiScale(grayFrame, faceDetections, 1.2, 4, 0,
                            new Size(120, 120), new Size(400, 400));

                    Rect[] faces = faceDetections.toArray();

                    if (faces.length > 0) {
                        // Prendre le visage le plus grand (le plus proche)
                        Rect largestFace = faces[0];
                        for (Rect rect : faces) {
                            if (rect.width * rect.height > largestFace.width * largestFace.height) {
                                largestFace = rect;
                            }
                        }

                        // Dessiner le rectangle de détection
                        Imgproc.rectangle(displayFrame,
                                new Point(largestFace.x, largestFace.y),
                                new Point(largestFace.x + largestFace.width, largestFace.y + largestFace.height),
                                new Scalar(0, 255, 0), 2);

                        if (frameCallback != null) {
                            frameCallback.accept(displayFrame);
                        }

                        // Extraire et redimensionner le visage
                        Mat face = new Mat(grayFrame, largestFace);
                        Imgproc.resize(face, resizedFace, new Size(FACE_SIZE, FACE_SIZE));

                        // Comparer avec tous les utilisateurs enregistrés
                        Integer matchedUserId = findMatchingUser(resizedFace);

                        if (matchedUserId != null) {
                            authenticatedUserId = matchedUserId;
                            System.out.println("Authentification réussie ! User ID: " + authenticatedUserId);
                            break;
                        }

                        // Libérer la mémoire
                        face.release();
                    }

                    faceDetections.release();
                }
            }

            attempts++;

            try {
                Thread.sleep(30); // ~30 FPS pour un affichage fluide
            } catch (InterruptedException e) {
                break;
            }
        }

        // Libérer toutes les ressources
        frame.release();
        grayFrame.release();
        displayFrame.release();
        resizedFace.release();
        stopCamera();

        return authenticatedUserId;
    }

    /**
     * Version sans callback (pour compatibilité)
     */
    public Integer authenticateWithFace() {
        return authenticateWithFace(null);
    }

    /**
     * Arrêter la caméra proprement
     */
    public void stopCamera() {
        if (currentCamera != null) {
            if (currentCamera.isOpened()) {
                currentCamera.release();
            }
            currentCamera = null;
        }
        // Forcer le garbage collector à nettoyer
        System.gc();
    }

    /**
     * Trouver l'utilisateur correspondant au visage
     */
    private Integer findMatchingUser(Mat testFace) {
        File dataDir = new File(FACES_DATA_DIR);
        if (!dataDir.exists()) {
            System.err.println("❌ Le dossier face_data/ n'existe pas");
            return null;
        }

        File[] userDirs = dataDir.listFiles();
        if (userDirs == null || userDirs.length == 0) {
            System.err.println("❌ Aucun utilisateur enregistré dans face_data/");
            return null;
        }

        System.out.println("🔍 Recherche parmi " + userDirs.length + " utilisateurs...");
        Map<Integer, Double> userSimilarities = new HashMap<>();

        // Parcourir tous les dossiers d'utilisateurs
        for (File userDir : userDirs) {
            if (userDir.isDirectory()) {
                try {
                    int userId = Integer.parseInt(userDir.getName());

                    File[] imageFiles = userDir.listFiles((dir, name) -> name.endsWith(".jpg"));
                    if (imageFiles == null || imageFiles.length == 0) {
                        System.err.println("⚠️ Aucune image trouvée pour l'utilisateur " + userId);
                        continue;
                    }

                    System.out.println("📸 Utilisateur " + userId + ": " + imageFiles.length + " images");

                    double totalSimilarity = 0;
                    int imageCount = 0;
                    double maxSimilarity = 0;

                    // Comparer avec toutes les images de cet utilisateur
                    for (File imageFile : imageFiles) {
                        Mat storedFace = Imgcodecs.imread(imageFile.getAbsolutePath(), Imgcodecs.IMREAD_GRAYSCALE);
                        if (!storedFace.empty()) {
                            double similarity = compareFaces(testFace, storedFace);
                            totalSimilarity += similarity;
                            imageCount++;

                            if (similarity > maxSimilarity) {
                                maxSimilarity = similarity;
                            }

                            storedFace.release();
                        }
                    }

                    if (imageCount > 0) {
                        double avgSimilarity = totalSimilarity / imageCount;
                        // Utiliser la moyenne pondérée entre moyenne et max
                        double finalScore = (avgSimilarity * 0.7) + (maxSimilarity * 0.3);
                        userSimilarities.put(userId, finalScore);

                        System.out.println("👤 User " + userId + " - Score: " +
                                String.format("%.2f%%", finalScore * 100) +
                                " (avg: " + String.format("%.2f%%", avgSimilarity * 100) +
                                ", max: " + String.format("%.2f%%", maxSimilarity * 100) + ")");
                    }
                } catch (NumberFormatException e) {
                    // Ignorer les dossiers qui ne sont pas des IDs
                    System.err.println("⚠️ Dossier ignoré: " + userDir.getName());
                }
            }
        }

        // Trouver l'utilisateur avec la plus grande similarité
        Integer bestMatch = null;
        double bestSimilarity = 0;

        for (Map.Entry<Integer, Double> entry : userSimilarities.entrySet()) {
            if (entry.getValue() > bestSimilarity) {
                bestSimilarity = entry.getValue();
                bestMatch = entry.getKey();
            }
        }

        // Retourner l'utilisateur si la similarité dépasse le seuil
        if (bestMatch != null && bestSimilarity >= SIMILARITY_THRESHOLD) {
            System.out.println("✅ MATCH TROUVÉ - User ID: " + bestMatch + ", Score: " +
                    String.format("%.2f%%", bestSimilarity * 100) + " (seuil: " +
                    String.format("%.2f%%", SIMILARITY_THRESHOLD * 100) + ")");
            return bestMatch;
        } else if (bestMatch != null) {
            System.out.println("❌ MEILLEUR MATCH INSUFFISANT - User ID: " + bestMatch + ", Score: " +
                    String.format("%.2f%%", bestSimilarity * 100) + " (seuil: " +
                    String.format("%.2f%%", SIMILARITY_THRESHOLD * 100) + ")");
        } else {
            System.out.println("❌ Aucun match trouvé");
        }

        return null;
    }

    /**
     * Comparer deux visages et retourner un score de similarité (0.0 à 1.0)
     */
    private double compareFaces(Mat face1, Mat face2) {
        try {
            // Vérifier que les images ont la même taille
            if (face1.size().width != face2.size().width || face1.size().height != face2.size().height) {
                Mat resized = new Mat();
                Imgproc.resize(face2, resized, face1.size());
                face2 = resized;
            }

            // Méthode 1: Corrélation par template matching
            Mat result = new Mat();
            Imgproc.matchTemplate(face1, face2, result, Imgproc.TM_CCOEFF_NORMED);
            Core.MinMaxLocResult mmr = Core.minMaxLoc(result);
            double correlation = Math.max(0, mmr.maxVal); // S'assurer que c'est positif
            result.release();

            // Méthode 2: Comparaison d'histogrammes
            Mat hist1 = new Mat();
            Mat hist2 = new Mat();

            MatOfInt histSize = new MatOfInt(256);
            MatOfFloat ranges = new MatOfFloat(0f, 256f);
            MatOfInt channels = new MatOfInt(0);

            Imgproc.calcHist(Arrays.asList(face1), channels, new Mat(), hist1, histSize, ranges);
            Imgproc.calcHist(Arrays.asList(face2), channels, new Mat(), hist2, histSize, ranges);

            Core.normalize(hist1, hist1, 0, 1, Core.NORM_MINMAX);
            Core.normalize(hist2, hist2, 0, 1, Core.NORM_MINMAX);

            double histSimilarity = Imgproc.compareHist(hist1, hist2, Imgproc.HISTCMP_CORREL);
            histSimilarity = Math.max(0, histSimilarity); // S'assurer que c'est positif

            hist1.release();
            hist2.release();

            // Méthode 3: Différence absolue moyenne (inversée)
            Mat diff = new Mat();
            Core.absdiff(face1, face2, diff);
            Scalar meanDiff = Core.mean(diff);
            double avgDiff = meanDiff.val[0];
            double diffSimilarity = 1.0 - (avgDiff / 255.0); // Normaliser entre 0 et 1
            diff.release();

            // Combiner les trois méthodes avec pondération
            double finalScore = (correlation * 0.4) + (histSimilarity * 0.4) + (diffSimilarity * 0.2);

            return Math.min(1.0, Math.max(0.0, finalScore)); // Borner entre 0 et 1

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la comparaison: " + e.getMessage());
            return 0.0;
        }
    }

    /**
     * Sauvegarder les données faciales d'un utilisateur (version publique)
     */
    public void saveFaceDataPublic(int userId, List<Mat> faces) {
        saveFaceData(userId, faces);
    }

    /**
     * Sauvegarder les données faciales d'un utilisateur
     */
    private void saveFaceData(int userId, List<Mat> faces) {
        String userDir = FACES_DATA_DIR + userId + "/";
        File dir = new File(userDir);

        // Supprimer l'ancien dossier s'il existe
        if (dir.exists()) {
            deleteDirectory(dir);
        }

        // Créer le nouveau dossier
        dir.mkdirs();

        System.out.println("📁 Sauvegarde des données faciales dans: " + userDir);

        int savedCount = 0;
        for (int i = 0; i < faces.size(); i++) {
            String filename = userDir + "face_" + i + ".jpg";
            boolean success = Imgcodecs.imwrite(filename, faces.get(i));
            if (success) {
                savedCount++;
                System.out.println("✅ Image sauvegardée: " + filename);
            } else {
                System.err.println("❌ Échec sauvegarde: " + filename);
            }
        }

        System.out.println("📊 Total images sauvegardées: " + savedCount + "/" + faces.size());

        // Enregistrer l'ID dans la base de données
        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                    "UPDATE users SET face_id_enrolled = true WHERE id_user = ?"
            );
            ps.setInt(1, userId);
            int rows = ps.executeUpdate();

            if (rows > 0) {
                System.out.println("✅ Face ID enregistré dans la base de données pour l'utilisateur " + userId);
            } else {
                System.err.println("❌ Échec mise à jour base de données pour l'utilisateur " + userId);
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur base de données:");
            e.printStackTrace();
        }

        // Vérifier que les fichiers existent bien
        File[] savedFiles = dir.listFiles();
        if (savedFiles != null) {
            System.out.println("📂 Fichiers dans le dossier: " + savedFiles.length);
        }
    }

    /**
     * Vérifier si un utilisateur a déjà enregistré son visage
     */
    public static boolean isFaceIDEnrolled(int userId) {
        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT face_id_enrolled FROM users WHERE id_user = ?"
            );
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getBoolean("face_id_enrolled");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Supprimer les données faciales d'un utilisateur
     */
    public static boolean deleteFaceData(int userId) {
        try {
            String userDir = FACES_DATA_DIR + userId;
            File directory = new File(userDir);

            if (directory.exists()) {
                deleteDirectory(directory);
            }

            // Mettre à jour la base de données
            try (Connection conn = DBConnection.getConnection()) {
                PreparedStatement ps = conn.prepareStatement(
                        "UPDATE users SET face_id_enrolled = false WHERE id_user = ?"
                );
                ps.setInt(1, userId);
                ps.executeUpdate();
            }

            System.out.println("Données Face ID supprimées pour l'utilisateur " + userId);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }

    /**
     * Capturer une frame pour l'affichage
     */
    public Mat captureFrame() {
        VideoCapture camera = new VideoCapture(0);
        Mat frame = new Mat();

        if (camera.isOpened()) {
            camera.read(frame);
            camera.release();
        }

        return frame;
    }
}