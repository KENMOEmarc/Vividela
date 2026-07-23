package com.template.auth.service.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import com.template.auth.config.ShopProperties;
import com.template.auth.model.entity.Article;
import com.template.auth.model.entity.ArticleService;
import com.template.auth.model.entity.Order;
import com.template.auth.model.entity.User;
import com.template.auth.model.enums.ClothingType;
import com.template.auth.model.enums.PaymentStatus;
import com.template.auth.model.enums.ServiceType;
import com.template.auth.model.enums.SizeType;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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

    // Largeur façon ticket de caisse (~80mm) ; hauteur généreuse, le contenu
    // ne remplit que ce dont il a besoin.
    private static final Rectangle PAGE_SIZE = new Rectangle(226f, 750f);

    private static final DateTimeFormatter DATE_FMT     = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm");

    private ReceiptPdfGenerator() {
        // Classe utilitaire : pas d'instanciation
    }

    /**
     * Génère le reçu de vente au format compact.
     *
     * @param order     la commande facturée
     * @param articles  les vêtements de la commande
     * @param services  les services appliqués (facturés) sur ces vêtements
     * @param reference numéro de facture à afficher (= nom du fichier PDF)
     * @param shop      identité de la boutique (nom/téléphone/email), externalisée
     *                  dans application.yml (vividela.shop.*) plutôt qu'en dur.
     * @param issuedBy  utilisateur actuellement authentifié qui a demandé la
     *                  génération de ce reçu (affiché comme "Émis par"). Peut
     *                  être {@code null} si l'information n'est pas disponible.
     * @param netAmountDue montant net réellement dû (remise ET points de
     *                  fidélité utilisés déjà déduits), calculé par
     *                  TicketServiceImpl avec la même formule que
     *                  OrderDto.netAmountDue — garantit que l'API et ce PDF
     *                  affichent toujours exactement le même montant. Voir
     *                  revue de code, règle manquante n°13.
     */
    public static byte[] generate(Order order, List<Article> articles, List<ArticleService> services,
                                   String reference, ShopProperties shop, User issuedBy, BigDecimal netAmountDue) {
        try {
            Document document = new Document(PAGE_SIZE, 14f, 14f, 12f, 12f);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, out);
            document.open();

            Font shopFont      = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, Color.BLACK);
            Font contactFont   = FontFactory.getFont(FontFactory.HELVETICA, 7.5f, Color.DARK_GRAY);
            Font labelFont     = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, Color.BLACK);
            Font refFont       = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9.5f, new Color(15, 118, 110));
            Font normalFont    = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, Color.BLACK);
            Font articleFont   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, new Color(15, 118, 110));
            Font serviceFont   = FontFactory.getFont(FontFactory.HELVETICA, 8f, Color.BLACK);
            Font subtotalFont  = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 7.5f, Color.DARK_GRAY);
            Font totalFont     = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10.5f, Color.BLACK);
            Font smallFont     = FontFactory.getFont(FontFactory.HELVETICA, 7.5f, Color.GRAY);
            Font statusFont;

            // ── En-tête établissement ───────────────────────────────────
            addCentered(document, shop.getName(), shopFont, 0f, 1f);
            addCentered(document, shop.getPhone(), contactFont, 0f, 0f);
            addCentered(document, shop.getEmail(), contactFont, 0f, 4f);

            document.add(dashedSeparator());

            addCentered(document, "REÇU DE VENTE", labelFont, 4f, 1f);
            addCentered(document, "N° " + reference, refFont, 0f, 6f);

            // ── Informations commande ───────────────────────────────────
            User client = order.getClientUser();
            String clientName = client != null
                    ? (safe(client.getFirstName()) + " " + safe(client.getLastName())).trim()
                    : "-";

            addInfoLine(document, "Client", clientName, labelFont, normalFont);
            addInfoLine(document, "Commande", "N°" + order.getId(), labelFont, normalFont);
            addInfoLine(document, "Date", DATETIME_FMT.format(Instant.now().atZone(ZoneId.systemDefault())), labelFont, normalFont);
            if (order.getCreatedBy() != null) {
                String cashier = (safe(order.getCreatedBy().getFirstName()) + " " + safe(order.getCreatedBy().getLastName())).trim();
                if (!cashier.isEmpty()) {
                    addInfoLine(document, "Caissier", cashier, labelFont, normalFont);
                }
            }
            if (issuedBy != null) {
                String issuer = (safe(issuedBy.getFirstName()) + " " + safe(issuedBy.getLastName())).trim();
                if (!issuer.isEmpty()) {
                    addInfoLine(document, "Émis par", issuer, labelFont, normalFont);
                }
            }

            Paragraph spacer = new Paragraph(" ", smallFont);
            spacer.setSpacingAfter(2);
            document.add(spacer);
            document.add(dashedSeparator());

            // ── Détail par article : chaque vêtement avec les services
            //    qui lui sont appliqués et leur prix ────────────────────────
            Map<Long, List<ArticleService>> servicesByArticle = new LinkedHashMap<>();
            if (services != null) {
                for (ArticleService service : services) {
                    Long articleId = service.getArticle() != null ? service.getArticle().getId() : null;
                    servicesByArticle.computeIfAbsent(articleId, k -> new java.util.ArrayList<>()).add(service);
                }
            }

            BigDecimal computedTotal = BigDecimal.ZERO;
            int articleIndex = 0;
            for (Article article : articles) {
                articleIndex++;
                List<ArticleService> articleServices = servicesByArticle.get(article.getId());

                Paragraph articleTitle = new Paragraph(articleIndex + ". " + formatArticleLabel(article), articleFont);
                articleTitle.setSpacingBefore(articleIndex == 1 ? 0f : 5f);
                articleTitle.setSpacingAfter(2f);
                document.add(articleTitle);

                PdfPTable serviceTable = new PdfPTable(2);
                serviceTable.setWidthPercentage(100);
                serviceTable.setWidths(new float[]{2.6f, 1.4f});
                serviceTable.setSpacingAfter(1f);

                BigDecimal articleTotal = BigDecimal.ZERO;
                if (articleServices != null && !articleServices.isEmpty()) {
                    for (ArticleService as : articleServices) {
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
            BigDecimal total    = order.getTotalAmount() != null ? order.getTotalAmount() : computedTotal;
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

            Paragraph netSep = new Paragraph(" ", smallFont);
            netSep.setSpacingAfter(1);
            document.add(netSep);
            addTotalLine(document, "NET À PAYER", netTotal.toPlainString() + " FCFA", totalFont, totalFont);

            // ── Statut de paiement ───────────────────────────────────────
            PaymentStatus effectiveStatus = order.getPaymentStatus() != null ? order.getPaymentStatus() : PaymentStatus.PENDING;
            String paymentLabel;
            Color statusColor;
            switch (effectiveStatus) {
                case COMPLETED:
                    paymentLabel = "PAYÉ";
                    statusColor = new Color(21, 128, 61);
                    break;
                case FAILED:
                    paymentLabel = "PAIEMENT ÉCHOUÉ";
                    statusColor = new Color(185, 28, 28);
                    break;
                case REFUNDED:
                    paymentLabel = "REMBOURSÉ";
                    statusColor = new Color(71, 85, 105);
                    break;
                default:
                    paymentLabel = "EN ATTENTE DE PAIEMENT";
                    statusColor = new Color(180, 83, 9);
            }
            statusFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, statusColor);
            addCentered(document, paymentLabel, statusFont, 8f, 8f);

            // ── Signatures ───────────────────────────────────────────────
            PdfPTable signTable = new PdfPTable(2);
            signTable.setWidthPercentage(100);
            signTable.setSpacingBefore(6);
            signTable.setSpacingAfter(6);

            Font signFont = FontFactory.getFont(FontFactory.HELVETICA, 7.5f, Color.GRAY);
            addSignatureCell(signTable, "Signature client", signFont);
            addSignatureCell(signTable, "Signature caisse", signFont);
            document.add(signTable);

            document.add(dashedSeparator());
            addCentered(document, "Merci pour votre achat", contactFont, 4f, 0f);

            document.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new IllegalStateException("Erreur lors de la génération du reçu PDF : " + e.getMessage(), e);
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────

    /** Libellé complet d'un article : type de vêtement, taille et éventuelle distinction. */
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
        switch (type) {
            case SHIRT:      return "Chemise";
            case T_SHIRT:    return "T-shirt";
            case PANTS:      return "Pantalon";
            case JEANS:      return "Jeans";
            case SKIRT:      return "Jupe";
            case DRESS:      return "Robe";
            case JACKET:     return "Veste";
            case COAT:       return "Manteau";
            case SWEATER:    return "Pull";
            case BLOUSE:     return "Blouse";
            case UNDERWEAR:  return "Sous-vêtement";
            case SOCKS:      return "Chaussettes";
            case SUIT:       return "Costume";
            case VEST:       return "Gilet";
            case SHORTS:     return "Short";
            case SCARF:      return "Écharpe";
            case GLOVES:     return "Gants";
            case BELT:       return "Ceinture";
            default:         return type.name();
        }
    }

    private static String formatSizeType(SizeType size) {
        return size == SizeType.UNIQUE ? "Taille unique" : size.name();
    }

    private static String formatServiceType(ServiceType type) {
        switch (type) {
            case DRY_CLEAN:
                return "Nettoyage à sec";
            case WASH:
                return "Lavage";
            case IRON:
                return "Repassage";
            case STAIN_REMOVAL:
                return "Détachage";
            case DEYING:
                return "Teinture";
            case ALTERATION:
                return "Retouche";
            default:
                return type.name();
        }
    }

    private static void addCentered(Document document, String text, Font font, float before, float after) throws DocumentException {
        Paragraph p = new Paragraph(text, font);
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingBefore(before);
        p.setSpacingAfter(after);
        document.add(p);
    }

    private static void addInfoLine(Document document, String label, String value, Font labelFont, Font valueFont) throws DocumentException {
        Paragraph p = new Paragraph();
        p.add(new com.lowagie.text.Chunk(label + " : ", labelFont));
        p.add(new com.lowagie.text.Chunk(value, valueFont));
        p.setSpacingAfter(1.5f);
        document.add(p);
    }

    private static void addTotalLine(Document document, String label, String value, Font labelFont, Font valueFont) throws DocumentException {
        PdfPTable row = new PdfPTable(2);
        row.setWidthPercentage(100);
        row.setWidths(new float[]{1f, 1f});

        PdfPCell labelCell = new PdfPCell(new com.lowagie.text.Phrase(label, labelFont));
        labelCell.setBorder(0);
        labelCell.setPadding(1);
        row.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new com.lowagie.text.Phrase(value, valueFont));
        valueCell.setBorder(0);
        valueCell.setPadding(1);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        row.addCell(valueCell);

        document.add(row);
    }

    private static void addServiceRow(PdfPTable table, String label, String value, Font font) {
        PdfPCell labelCell = new PdfPCell(new com.lowagie.text.Phrase(label, font));
        labelCell.setBorder(0);
        labelCell.setPadding(1.5f);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new com.lowagie.text.Phrase(value, font));
        valueCell.setBorder(0);
        valueCell.setPadding(1.5f);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(valueCell);
    }

    private static void addSignatureCell(PdfPTable table, String label, Font font) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(0);
        cell.setPaddingTop(14);
        Paragraph p = new Paragraph(label + "\nNom et cachet", font);
        p.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(p);
        table.addCell(cell);
    }

    private static LineSeparator dashedSeparator() {
        LineSeparator separator = new LineSeparator();
        separator.setLineColor(Color.LIGHT_GRAY);
        separator.setLineWidth(0.6f);
        return separator;
    }

    private static String safe(String value) {
        return value != null ? value : "";
    }
}
