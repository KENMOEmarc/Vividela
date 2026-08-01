package com.template.vivid.common.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.Barcode128;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.template.vivid.model.entity.Article;
import com.template.vivid.model.entity.Order;
import com.template.vivid.model.entity.Ticket;
import org.springframework.beans.factory.annotation.Value;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * Génère le PDF du ticket de dépôt d'une commande (Vividela).
 * Le ticket est DÉTACHABLE : la partie supérieure contient le code-barres et le nombre de vêtements.
 * Librairie utilisée : OpenPDF (100% Java, aucune dépendance native).
 */
public final class TicketPdfGenerator {

    @Value("${vividela.shop.name}")
    private static String shopName;

    private TicketPdfGenerator() {
        // Classe utilitaire : pas d'instanciation
    }

    /**
     * Génère un ticket détachable simple avec code-barres et nombre de vêtements.
     * Destiné à être imprimé, découpé et remis au client.
     */
    public static byte[] generate(Order order, List<Article> articles, Ticket ticket) {
        try {
            Document document = new Document(PageSize.A5);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new Color(30, 64, 175));
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
            Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);

            // ─── PARTIE SUPÉRIEURE : CODE-BARRES ET INFORMATION CLÉS ─────────────
            Paragraph title = new Paragraph(shopName, titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph ticketLabel = new Paragraph("TICKET DE DÉPÔT", smallFont);
            ticketLabel.setAlignment(Element.ALIGN_CENTER);
            ticketLabel.setSpacingAfter(10);
            document.add(ticketLabel);

            // Code-barres (partie personnel — à conserver en caisse/atelier)
            boolean hasBarcode = ticket.getBarcode() != null && !ticket.getBarcode().trim().isEmpty();
            if (hasBarcode) {
                addBarcode(document, writer, ticket.getBarcode(), smallFont);
            }

            // Infos compactes
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setSpacingBefore(0);
            infoTable.setSpacingAfter(8);

            addRow(infoTable, "Commande n°", String.valueOf(order.getId()), boldFont, normalFont);
            addRow(infoTable, "Vêtements", String.valueOf(articles.size()), boldFont, normalFont);

            document.add(infoTable);

            // ─── LIGNE DE SÉPARATION POINTILLÉE ─────────────────────────────────
            Paragraph separator = new Paragraph("┄ À DÉTACHER ┄", smallFont);
            separator.setAlignment(Element.ALIGN_CENTER);
            separator.setSpacingBefore(10);
            separator.setSpacingAfter(12);
            document.add(separator);

            // ─── PARTIE INFÉRIEURE : CONSERVATION CLIENT ─────────────────────────
            Paragraph detachTitle = new Paragraph("À CONSERVER", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(15, 118, 110)));
            detachTitle.setAlignment(Element.ALIGN_CENTER);
            detachTitle.setSpacingAfter(6);
            document.add(detachTitle);

            // Code-barres (partie client — à conserver et présenter au retrait)
            if (hasBarcode) {
                addBarcode(document, writer, ticket.getBarcode(), smallFont);
            }

            Paragraph barcodeRepeat = new Paragraph("Commande n° " + order.getId(), normalFont);
            barcodeRepeat.setAlignment(Element.ALIGN_CENTER);
            barcodeRepeat.setSpacingAfter(4);
            document.add(barcodeRepeat);

            Paragraph itemCount = new Paragraph(articles.size() + " vêtement" + (articles.size() > 1 ? "s" : ""), normalFont);
            itemCount.setAlignment(Element.ALIGN_CENTER);
            itemCount.setSpacingAfter(8);
            document.add(itemCount);

            Paragraph instructions = new Paragraph("Présentez ce ticket lors du retrait", smallFont);
            instructions.setAlignment(Element.ALIGN_CENTER);
            instructions.setSpacingAfter(4);
            document.add(instructions);

            Paragraph footer = new Paragraph(shopName, smallFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new IllegalStateException("Erreur lors de la génération du ticket PDF : " + e.getMessage(), e);
        }
    }

    /** Ajoute l'image du code-barres suivie de sa valeur textuelle, centrées. */
    private static void addBarcode(Document document, PdfWriter writer, String code, Font codeFont) throws DocumentException {
        Barcode128 barcode128 = new Barcode128();
        barcode128.setCode(code);
        barcode128.setBarHeight(35f);
        Image barcodeImage = barcode128.createImageWithBarcode(writer.getDirectContent(), Color.BLACK, Color.BLACK);
        barcodeImage.setAlignment(Element.ALIGN_CENTER);
        barcodeImage.scaleToFit(140, 50);
        document.add(barcodeImage);

        Paragraph barcodeText = new Paragraph(code, codeFont);
        barcodeText.setAlignment(Element.ALIGN_CENTER);
        barcodeText.setSpacingAfter(12);
        document.add(barcodeText);
    }

    private static void addRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new com.lowagie.text.Phrase(label, labelFont));
        labelCell.setBorder(0);
        labelCell.setPadding(2);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new com.lowagie.text.Phrase(value, valueFont));
        valueCell.setBorder(0);
        valueCell.setPadding(2);
        table.addCell(valueCell);
    }
}
