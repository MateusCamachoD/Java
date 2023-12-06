import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Set;

public class Main {
    private static Set<String> addedTickers = new HashSet<>();

    public static void main(String[] args) {
        JFrame frame = new JFrame("CCH Mercado Bitcoin");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(470, 500);

        frame.setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new FlowLayout());
        JPanel bottomPanel = new JPanel(new BorderLayout());

        JLabel labelTicker = new JLabel("Ticker:");
        JTextField textFieldTicker = new JTextField(10);
        JButton buttonAdd = new JButton("Adicionar");
        JButton buttonRemove = new JButton("Remover");
        JButton buttonUpdate = new JButton("Atualizar");

        String[] columnNames = {"Ticker", "Compra", "Venda"};
        DefaultTableModel model = new DefaultTableModel(columnNames, 0);
        JTable table = new JTable(model);

        DefaultTableCellRenderer leftRenderer = new DefaultTableCellRenderer();
        leftRenderer.setHorizontalAlignment(JLabel.LEFT);
        table.getColumnModel().getColumn(1).setCellRenderer(leftRenderer); 
        table.getColumnModel().getColumn(2).setCellRenderer(leftRenderer); 

        JScrollPane scrollPane = new JScrollPane(table);

        topPanel.add(labelTicker);
        topPanel.add(textFieldTicker);
        topPanel.add(buttonAdd);
        topPanel.add(buttonRemove);
        topPanel.add(buttonUpdate);

        bottomPanel.add(scrollPane, BorderLayout.CENTER);

        frame.add(topPanel, BorderLayout.PAGE_START);
        frame.add(bottomPanel, BorderLayout.CENTER);

        buttonAdd.addActionListener((ActionEvent e) -> {
            String ticker = textFieldTicker.getText().toUpperCase();
            if (ticker.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Preencha o campo Ticker antes de adicionar");
            } else if (addedTickers.contains(ticker)) {
                JOptionPane.showMessageDialog(frame, "Esse ticker já foi adicionado à tabela");
            } else {
                try {
                    TickerData tickerData = getTickerData(ticker);
                    if (tickerData != null) {
                        Object[] row = {ticker, tickerData.getFormattedBuy(), tickerData.getFormattedSell()};
                        model.addRow(row);
                        addedTickers.add(ticker);
                    } else {
                        JOptionPane.showMessageDialog(frame, "Falha ao tentar requisitar Data do Ticker " + ticker);
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

        buttonRemove.addActionListener((ActionEvent e) -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow != -1) {
                String removedTicker = (String) model.getValueAt(selectedRow, 0);
                model.removeRow(selectedRow);
                addedTickers.remove(removedTicker);
            } else {
                JOptionPane.showMessageDialog(frame, "Selecione uma linha para remover");
            }
        });

        buttonUpdate.addActionListener((ActionEvent e) -> {
            for (int row = 0; row < model.getRowCount(); row++) {
                String ticker = (String) model.getValueAt(row, 0);
                try {
                    TickerData tickerData = getTickerData(ticker);
                    if (tickerData != null) {
                        model.setValueAt(tickerData.getFormattedBuy(), row, 1);
                        model.setValueAt(tickerData.getFormattedSell(), row, 2);
                    } else {
                        JOptionPane.showMessageDialog(frame, "Falha ao tentar requisitar Data do Ticker " + ticker);
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

        frame.setVisible(true);
    }

    private static TickerData getTickerData(String ticker) throws IOException {
        String apiUrl = "https://www.mercadobitcoin.net/api/" + ticker + "/ticker";

        HttpURLConnection connection = null;
        BufferedReader reader = null;
        try {
            URI url = new URI(apiUrl);
            connection = (HttpURLConnection) url.toURL().openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                double buy = getValueFromResponse(response.toString(), "buy");
                double sell = getValueFromResponse(response.toString(), "sell");

                return new TickerData(buy, sell);
            } else {
                System.out.println("Falha ao tentar requisitar Data da API: " + responseCode);
            }
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                reader.close();
            }
            if (connection != null) {
                connection.disconnect();
            }
        }

        return null;
    }

    private static double getValueFromResponse(String response, String key) {
        int keyIndex = response.indexOf("\"" + key + "\":");
        if (keyIndex != -1) {
            int startIndex = keyIndex + key.length() + 3;
            int endIndex = response.indexOf(",", startIndex);
            if (endIndex == -1) {
                endIndex = response.indexOf("}", startIndex);
            }

            String valueString = response.substring(startIndex, endIndex).replace("\"", "");
            return Double.parseDouble(valueString);
        } else {
            System.out.println("Key not found in response: " + key);
            return 0.0;
        }
    }
}

class TickerData {
    private double buy;
    private double sell;
    private static final DecimalFormat currencyFormat = new DecimalFormat("#,##0.00");

    public TickerData(double buy, double sell) {
        this.buy = buy;
        this.sell = sell;
    }

    public double getBuy() {
        return buy;
    }

    public double getSell() {
        return sell;
    }

    public String getFormattedBuy() {
        return "R$ " + currencyFormat.format(buy);
    }

    public String getFormattedSell() {
        return "R$ " + currencyFormat.format(sell);
    }
}