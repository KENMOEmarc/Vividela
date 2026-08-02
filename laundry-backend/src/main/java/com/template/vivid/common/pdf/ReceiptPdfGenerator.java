package com.template.vivid.common.pdf;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import com.template.vivid.model.entity.Article;
import com.template.vivid.model.entity.ArticleServiceLine;
import com.template.vivid.model.entity.Order;
import com.template.vivid.model.entity.User;
import com.template.vivid.model.enums.ClothingType;
import com.template.vivid.model.enums.PaymentStatus;
import com.template.vivid.model.enums.ServiceType;
import com.template.vivid.model.enums.SizeType;
import org.springframework.beans.factory.annotation.Value;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Génère le PDF du reçu de vente d'une commande (Vividela), au format
 * compact d'un ticket de caisse (proche d'un ticket imprimante thermique).
 * Le numéro affiché en en-tête ("Reçu N° ...") correspond exactement à la
 * référence utilisée comme nom de fichier (voir {@link DocumentReferenceGenerator}).
 * Le détail est présenté article par article : chaque vêtement de la commande
 * est listé avec l'ensemble des services qui lui ont été appliqués et le prix
 * facturé pour chacun d'eux.
 * Librairie utilisée : OpenPDF (100% Java, aucune dépendance native).
 */
public final class ReceiptPdfGenerator {

    private static final Rectangle PAGE_SIZE = PageSize.A5;

    // Palette de couleurs utilisée pour le PDF
    private static final Color PRIMARY = new Color(14, 90, 138);    // principal bleu (en-tête, bandeaux)
    private static final Color PRIMARY_LIGHT = new Color(234, 244, 251);  // fond bleu clair (bandeau, zébrage)
    private static final Color BORDER_BLUE = new Color(170, 197, 214);  // bordures des tableaux
    private static final Color TEXT_DARK = new Color(31, 41, 55);
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm");
    @Value("${vividela.shop.name}")
    private static String shopName;
    @Value("${vividela.shop.phone}")
    private static String shopPhone;
    @Value("${vividela.shop.email}")
    private static String shopEmail;

    private ReceiptPdfGenerator() {
        // Classe utilitaire : pas d'instanciation
    }

    /**
     * Génère le reçu de vente au format compact.
     *
     * @param order        la commande facturée
     * @param articles     les vêtements de la commande
     * @param services     les services appliqués (facturés) sur ces vêtements
     * @param reference    numéro de facture à afficher (= nom du fichier PDF)
     * @param issuedBy     utilisateur actuellement authentifié qui a demandé la
     *                     génération de ce reçu (affiché comme "Émis par"). Peut
     *                     être {@code null} si l'information n'est pas disponible.
     * @param netAmountDue montant net réellement dû (remise et points de
     *                     fidélité utilisés déjà déduits), calculé par
     *                     TicketServiceImpl avec la même formule que
     *                     OrderDto.netAmountDue — garantit que l'API et ce PDF
     *                     affichent toujours exactement le même montant. Voir
     *                     revue de code, règle manquante n°13.
     */
    public static byte[] generate(Order order, List<Article> articles, List<ArticleServiceLine> services,
                                  String reference, User issuedBy, BigDecimal netAmountDue) {
        try {
            Document document = new Document(PAGE_SIZE, 20f, 20f, 16f, 16f);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, out);
            document.open();

            renderCopy(document, order, articles, services, reference, issuedBy, netAmountDue, "EXEMPLAIRE CLIENT");
            addCutLine(document);
            renderCopy(document, order, articles, services, reference, issuedBy, netAmountDue, "EXEMPLAIRE CAISSE");

            document.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new IllegalStateException("Erreur lors de la génération du reçu PDF : " + e.getMessage(), e);
        }
    }

    /**
     * Construit un exemplaire complet du reçu (client ou caisse) dans le document courant.
     */
    private static void renderCopy(Document document, Order order, List<Article> articles, List<ArticleServiceLine> services,
                                   String reference, User issuedBy, BigDecimal netAmountDue,
                                   String copyLabel) throws DocumentException {

        Font shopFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, PRIMARY);
        Font contactFont = FontFactory.getFont(FontFactory.HELVETICA, 7.5f, Color.DARK_GRAY);
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, TEXT_DARK);
        Font refFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9.5f, PRIMARY);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, TEXT_DARK);
        Font articleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, PRIMARY);
        Font serviceFont = FontFactory.getFont(FontFactory.HELVETICA, 8f, TEXT_DARK);
        Font subtotalFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 7.5f, Color.DARK_GRAY);
        Font statusFont;

        // ── En-tête établissement : bandeau bleu clair ──────────────────
        addBanner(document, shopName, shopFont);
        addCentered(document, shopPhone, contactFont, 1f, 0f);
        addCentered(document, shopEmail, contactFont, 0f, 5f);

        addCentered(document, "REÇU DE VENTE", labelFont, 2f, 1f);
        addCentered(document, "N° " + reference, refFont, 0f, 5f);

        // ── Badge "EXEMPLAIRE ..." (pastille bleue, comme sur le modèle) ─
        addBadge(document, copyLabel, 55f, 3f, 6f);

        // ── Informations commande (tableau zébré) ────────────────────────
        User client = order.getClientUser();
        String clientName = client != null
                ? (safe(client.getFirstName()) + " " + safe(client.getLastName())).trim()
                : "-";

        List<String[]> infoRows = new ArrayList<>();
        infoRows.add(new String[]{"Client", clientName.isEmpty() ? "-" : clientName});
        infoRows.add(new String[]{"Commande", "N°" + order.getId()});
        infoRows.add(new String[]{"Date", DATETIME_FMT.format(Instant.now().atZone(ZoneId.systemDefault()))});
        if (order.getCreatedBy() != null) {
            String cashier = (safe(order.getCreatedBy().getFirstName()) + " " + safe(order.getCreatedBy().getLastName())).trim();
            if (!cashier.isEmpty()) {
                infoRows.add(new String[]{"Caissier", cashier});
            }
        }
        if (issuedBy != null) {
            String issuer = (safe(issuedBy.getFirstName()) + " " + safe(issuedBy.getLastName())).trim();
            if (!issuer.isEmpty()) {
                infoRows.add(new String[]{"Émis par", issuer});
            }
        }
        addInfoTable(document, infoRows, labelFont, normalFont);

        // ── Détail par article : chaque vêtement avec les services
        //    qui lui sont appliqués et leur prix ────────────────────────
        Map<Long, List<ArticleServiceLine>> servicesByArticle = new LinkedHashMap<>();
        if (services != null) {
            for (ArticleServiceLine service : services) {
                Long articleId = service.getArticle() != null ? service.getArticle().getId() : null;
                servicesByArticle.computeIfAbsent(articleId, k -> new ArrayList<>()).add(service);
            }
        }

        BigDecimal computedTotal = BigDecimal.ZERO;
        int articleIndex = 0;
        for (Article article : articles) {
            articleIndex++;
            List<ArticleServiceLine> articleServices = servicesByArticle.get(article.getId());

            Paragraph articleTitle = new Paragraph(articleIndex + ". " + formatArticleLabel(article), articleFont);
            articleTitle.setSpacingBefore(articleIndex == 1 ? 2f : 6f);
            articleTitle.setSpacingAfter(2f);
            document.add(articleTitle);

            PdfPTable serviceTable = new PdfPTable(2);
            serviceTable.setWidthPercentage(100);
            serviceTable.setWidths(new float[]{2.6f, 1.4f});
            serviceTable.setSpacingAfter(1f);

            BigDecimal articleTotal = BigDecimal.ZERO;
            if (articleServices != null && !articleServices.isEmpty()) {
                for (ArticleServiceLine as : articleServices) {
                    BigDecimal price = as.getAppliedPrice() != null ? as.getAppliedPrice() : BigDecimal.ZERO;
                    articleTotal = articleTotal.add(price);
                    addServiceRow(serviceTable, "  • " + formatServiceType(as.getService()), price.toPlainString() + " F", serviceFont);
                }
            } else {
                addServiceRow(serviceTable, "  • Aucun service enregistré", "-", serviceFont);
            }
            document.add(serviceTable);

            computedTotal = computedTotal.add(articleTotal);

            Paragraph articleSubtotal = new Paragraph("Sous-total : " + articleTotal.toPlainString() + " F", subtotalFont);
            articleSubtotal.setAlignment(Element.ALIGN_RIGHT);
            articleSubtotal.setSpacingAfter(2f);
            document.add(articleSubtotal);
        }

        document.add(dashedSeparator());

        // ── Totaux ───────────────────────────────────────────────────
        BigDecimal total = order.getTotalAmount() != null ? order.getTotalAmount() : computedTotal;
        BigDecimal discount = order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO;
        // CORRECTION : le "NET À PAYER" utilisait auparavant total-discount
        // calculé localement, ignorant les points de fidélité utilisés —
        // ce qui pouvait diverger de l'API (OrderDto.netAmountDue). On
        // utilise désormais directement la valeur transmise par
        // TicketServiceImpl, seule source de vérité. Voir revue de code,
        // règle manquante n°13.
        BigDecimal netTotal = netAmountDue != null ? netAmountDue : total.subtract(discount);
        int loyaltyPointsUsed = order.getLoyaltyPointsUsed() != null ? order.getLoyaltyPointsUsed() : 0;

        addTotalLine(document, "Total brut", total.toPlainString() + " FCFA", normalFont, normalFont);
        if (discount.signum() > 0) {
            addTotalLine(document, "Remise", "- " + discount.toPlainString() + " FCFA", normalFont, normalFont);
        }
        if (loyaltyPointsUsed > 0) {
            addTotalLine(document, "Points fidélité utilisés", "-" + loyaltyPointsUsed + " pt(s)", normalFont, normalFont);
        }

        // ── Bandeau "NET À PAYER" mis en évidence, comme "MONTANT NET À
        //    PAYER" sur le document de référence ─────────────────────────
        addBanner(document, netTotal.toPlainString() + " FCFA");

        // ── Statut de paiement (pastille colorée) ────────────────────────
        PaymentStatus effectiveStatus = order.getPaymentStatus() != null ? order.getPaymentStatus() : PaymentStatus.PENDING;
        String paymentLabel;
        Color statusColor = switch (effectiveStatus) {
            case COMPLETED -> {
                paymentLabel = "PAYÉ";
                yield new Color(21, 128, 61);
            }
            case FAILED -> {
                paymentLabel = "PAIEMENT ÉCHOUÉ";
                yield new Color(185, 28, 28);
            }
            case REFUNDED -> {
                paymentLabel = "REMBOURSÉ";
                yield new Color(71, 85, 105);
            }
            default -> {
                paymentLabel = "EN ATTENTE DE PAIEMENT";
                yield new Color(180, 83, 9);
            }
        };
        statusFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8f, Color.WHITE);
        addColoredBadge(document, paymentLabel, statusColor, statusFont, 65f, 6f, 6f);

        // ── Signatures ───────────────────────────────────────────────
        PdfPTable signTable = new PdfPTable(2);
        signTable.setWidthPercentage(100);
        signTable.setSpacingBefore(6);
        signTable.setSpacingAfter(6);

        Font signFont = FontFactory.getFont(FontFactory.HELVETICA, 7.5f, Color.GRAY);
        addSignatureCell(signTable, "Signature client", signFont);
        addSignatureCell(signTable, "Signature caisse", signFont);
        document.add(signTable);

        addCentered(document, "Merci pour votre achat", contactFont, 2f, 0f);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────

    /**
     * Libellé complet d'un article : type de vêtement, taille et éventuelle distinction.
     */
    private static String formatArticleLabel(Article article) {
        StringBuilder sb = new StringBuilder();
        sb.append(article.getClothingType() != null ? formatClothingType(article.getClothingType()) : "Vêtement");
        if (article.getSize() != null) {
            sb.append(" (").append(formatSizeType(article.getSize())).append(")");
        }
        if (article.getDistinction() != null && !article.getDistinction().isBlank()) {
            sb.append(" - ").append(article.getDistinction().trim());
        }
        return sb.toString();
    }

    private static String formatClothingType(ClothingType type) {
        return switch (type) {
            case SHIRT -> "Chemise";
            case T_SHIRT -> "T-shirt";
            case PANTS -> "Pantalon";
            case JEANS -> "Jeans";
            case SKIRT -> "Jupe";
            case DRESS -> "Robe";
            case JACKET -> "Veste";
            case COAT -> "Manteau";
            case SWEATER -> "Pull";
            case BLOUSE -> "Blouse";
            case UNDERWEAR -> "Sous-vêtement";
            case SOCKS -> "Chaussettes";
            case SUIT -> "Costume";
            case VEST -> "Gilet";
            case SHORTS -> "Short";
            case SCARF -> "Écharpe";
            case GLOVES -> "Gants";
            case BELT -> "Ceinture";
        };
    }

    private static String formatSizeType(SizeType size) {
        return size == SizeType.UNIQUE ? "Taille unique" : size.name();
    }

    private static String formatServiceType(ServiceType type) {
        return switch (type) {
            case DRY_CLEAN -> "Nettoyage à sec";
            case WASH -> "Lavage";
            case IRON -> "Repassage";
            case STAIN_REMOVAL -> "Détachage";
            case DEYING -> "Teinture";
            case ALTERATION -> "Retouche";
        };
    }

    private static void addCentered(Document document, String text, Font font, float before, float after) throws DocumentException {
        Paragraph p = new Paragraph(text, font);
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingBefore(before);
        p.setSpacingAfter(after);
        document.add(p);
    }

    /**
     * Bandeau plein fond bleu (en-tête boutique, ou ligne "NET À PAYER" mise en évidence).
     */
    private static void addBanner(Document document, String text, Font labelFont) throws DocumentException {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingAfter(2f);
        PdfPCell cell = new PdfPCell(new Phrase(text, labelFont));
        cell.setBackgroundColor(PRIMARY_LIGHT);
        cell.setBorder(0);
        cell.setPadding(6f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        t.addCell(cell);
        document.add(t);
    }

    /**
     * Bandeau libellé/valeur en évidence (fond bleu plein, texte blanc) — ex. "NET À PAYER".
     */
    private static void addBanner(Document document, String value) throws DocumentException {
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{1.1f, 1f});
        t.setSpacingBefore(5f);
        t.setSpacingAfter(6f);

        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9.5f, Color.WHITE);
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11f, Color.WHITE);

        PdfPCell l = new PdfPCell(new Phrase("NET À PAYER", labelFont));
        l.setBackgroundColor(PRIMARY);
        l.setBorder(0);
        l.setPadding(6f);
        l.setVerticalAlignment(Element.ALIGN_MIDDLE);

        PdfPCell v = new PdfPCell(new Phrase(value, valueFont));
        v.setBackgroundColor(PRIMARY);
        v.setBorder(0);
        v.setPadding(6f);
        v.setHorizontalAlignment(Element.ALIGN_RIGHT);
        v.setVerticalAlignment(Element.ALIGN_MIDDLE);

        t.addCell(l);
        t.addCell(v);
        document.add(t);
    }

    /**
     * Pastille centrée bleue (ex. "EXEMPLAIRE CLIENT"), largeur réduite façon badge.
     */
    private static void addBadge(Document document, String text, float widthPercent, float before, float after) throws DocumentException {
        Font badgeFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8f, Color.WHITE);
        addColoredBadge(document, text, PRIMARY, badgeFont, widthPercent, before, after);
    }

    /**
     * Pastille centrée de couleur arbitraire (réutilisée pour le statut de paiement).
     */
    private static void addColoredBadge(Document document, String text, Color bg, Font font, float widthPercent, float before, float after) throws DocumentException {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(widthPercent);
        t.setHorizontalAlignment(Element.ALIGN_CENTER);
        t.setSpacingBefore(before);
        t.setSpacingAfter(after);
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bg);
        cell.setBorder(0);
        cell.setPadding(4f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        t.addCell(cell);
        document.add(t);
    }

    /**
     * Tableau d'informations zébré (fond bleu clair une ligne sur deux), bordures bleu clair.
     */
    private static void addInfoTable(Document document, List<String[]> rows, Font labelFont, Font valueFont) throws DocumentException {
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{1f, 1.4f});
        t.setSpacingBefore(3f);
        t.setSpacingAfter(4f);

        boolean alt = false;
        for (String[] row : rows) {
            Color bg = alt ? PRIMARY_LIGHT : Color.WHITE;

            PdfPCell l = new PdfPCell(new Phrase(row[0], labelFont));
            l.setBackgroundColor(bg);
            l.setBorderColor(BORDER_BLUE);
            l.setBorderWidth(0.4f);
            l.setPadding(3.5f);

            PdfPCell v = new PdfPCell(new Phrase(row[1], valueFont));
            v.setBackgroundColor(bg);
            v.setBorderColor(BORDER_BLUE);
            v.setBorderWidth(0.4f);
            v.setPadding(3.5f);
            v.setHorizontalAlignment(Element.ALIGN_RIGHT);

            t.addCell(l);
            t.addCell(v);
            alt = !alt;
        }
        document.add(t);
    }

    private static void addTotalLine(Document document, String label, String value, Font labelFont, Font valueFont) throws DocumentException {
        PdfPTable row = new PdfPTable(2);
        row.setWidthPercentage(100);
        row.setWidths(new float[]{1f, 1f});

        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(0);
        labelCell.setPadding(1);
        row.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(0);
        valueCell.setPadding(1);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        row.addCell(valueCell);

        document.add(row);
    }

    private static void addServiceRow(PdfPTable table, String label, String value, Font font) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, font));
        labelCell.setBorder(0);
        labelCell.setPadding(1.5f);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, font));
        valueCell.setBorder(0);
        valueCell.setPadding(1.5f);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(valueCell);
    }

    /**
     * Encadré de signature (bordure fine bleu clair), avec espace pour signer.
     */
    private static void addSignatureCell(PdfPTable table, String label, Font font) {
        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(BORDER_BLUE);
        cell.setBorderWidth(0.6f);
        cell.setPaddingTop(16);
        cell.setPaddingBottom(6);
        cell.setPaddingLeft(4);
        cell.setPaddingRight(4);
        Paragraph p = new Paragraph(label + "\nNom et cachet", font);
        p.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(p);
        table.addCell(cell);
    }

    private static LineSeparator dashedSeparator() {
        LineSeparator separator = new LineSeparator();
        separator.setLineColor(BORDER_BLUE);
        separator.setLineWidth(0.6f);
        return separator;
    }

    /**
     * Ligne de coupe entre les deux exemplaires (client / caisse), comme sur le modèle de référence.
     */
    private static void addCutLine(Document document) throws DocumentException {
        document.add(dashedSeparator());
        Font f = FontFactory.getFont(FontFactory.HELVETICA, 7.5f, Color.GRAY);
        addCentered(document, "✂  COUPER ICI  ✂", f, 3f, 3f);
        document.add(dashedSeparator());
    }

    private static String safe(String value) {
        return value != null ? value : "";
    }
}
