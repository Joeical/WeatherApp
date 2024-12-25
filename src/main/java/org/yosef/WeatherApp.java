package org.yosef;

import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.io.IOException;
import java.net.*;
import java.util.Scanner;

public class WeatherApp {
    // Variables to the URL of the weather data, and API Key.
    private static final String API_URL = "http://api.weatherapi.com/v1/current.json?key=";
    private static final String ALERTS_API_URL = "https://api.weatherapi.com/v1/alerts.json?key=";
    private static final String API_KEY = "f23b4c75bf2f4fa68e0122514241612";

    // Variables as a shortcut to setting the colors for the GUI, instead of writing the integers manually
    private static final Color DARK_BACKGROUND = new Color(30, 30, 30);
    private static final Color DARK_SECONDARY_BACKGROUND = new Color(45, 45, 45);
    private static final Color DARK_TEXT = new Color(220, 220, 220);
    private static final Color ACCENT_COLOR = new Color(100, 170, 255);

    public static void main(String[] args) { //this launches the UI in a new thread.
        SwingUtilities.invokeLater(WeatherApp::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        try {
            //this applies the default windows style
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            //Applies the color variables to the Panel
            UIManager.put("Panel.background", DARK_BACKGROUND);
            UIManager.put("ScrollPane.background", DARK_BACKGROUND);
            UIManager.put("ScrollPane.foreground", DARK_TEXT);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //These control the window's size, background color,
        JFrame frame = new JFrame("Weather App | [By Yousef Amr 2024]");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 720);
        frame.getContentPane().setBackground(DARK_BACKGROUND);
        frame.setLayout(new BorderLayout(10, 10));

        // This is the input place for the city, it stores the information about the looks of the Panel in memory, and adds it later
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10)); //Flow layout means it sets the ui elements one after another, and sets the Horizontal and vertical gape to 10
        inputPanel.setOpaque(false);
        inputPanel.setBackground(DARK_BACKGROUND);

        JTextField cityField = createDarkTextField("Enter City");
        JButton fetchWeatherButton = createDarkButton("Get Weather");
        JLabel weatherIconLabel = new JLabel();
        // sets the dimensions of the weather image.
        weatherIconLabel.setHorizontalAlignment(JLabel.CENTER);
        weatherIconLabel.setPreferredSize(new Dimension(48, 48));
        //This is where it adds the panel
        inputPanel.add(cityField);
        inputPanel.add(fetchWeatherButton);
        inputPanel.add(weatherIconLabel, BorderLayout.NORTH);

        // Where the weather data is shown.
        JTextArea weatherInfoArea = new JTextArea(5, 30);
        weatherInfoArea.setEditable(false);
        weatherInfoArea.setFont(new Font("Tahoma", Font.PLAIN, 14));
        weatherInfoArea.setBackground(DARK_SECONDARY_BACKGROUND);
        weatherInfoArea.setForeground(DARK_TEXT);
        weatherInfoArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60, 60, 60), 1), //This one controls the little lines around the frame
                BorderFactory.createEmptyBorder(10, 10, 10, 10) //Controls the size of the border
        ));

        frame.add(inputPanel, BorderLayout.PAGE_START); //This arranges the layout of the border at the top of the application

        frame.add(new JScrollPane(weatherInfoArea), BorderLayout.CENTER); //This does the same, but to the border around the where it shows the weather data

        // Shortcut for when entering a city, it listens for the default action, which is enter, and it clicks the button.
        cityField.addActionListener(e -> fetchWeatherButton.doClick());

        fetchWeatherButton.addActionListener(e -> {
            String city = cityField.getText().trim(); //This catches the city you input, and trims it; which means to cut out the spaces around the city name
            if (!city.isEmpty()) {
                weatherInfoArea.setText("Fetching weather...");
                weatherIconLabel.setIcon(null); //This resets the icon, so that it is ready for the next one.
                new SwingWorker<WeatherData, Void>() { //this is a template class named SwingWorker, it has nothing in it but we can choose what it does, in this case
                    @Override  //This overrides the doinbackground method, to not make the application freeze for no reason.
                    // this is a background thread, it takes nothing and returns the weather data in later.
                    protected WeatherData doInBackground() {
                        return getWeatherData(city);
                    }

                    @Override
                    protected void done() {
                        try {
                            WeatherData weatherData = get(); //This gets the weather data later on.
                            StringBuilder sb = new StringBuilder();
                            sb.append(weatherData.weatherText); //adds to the variable sb the weatherdata and the text, to type out the information
                            System.out.println(city); //Prints out the information
                            sb.append(fetchAlerts(city)); //Prints out the alerts right after
                            weatherInfoArea.setText(sb.toString()); //sets the printed information actually show up, inside the frame

                            // Set icon from URL
                            if (weatherData.iconUrl != null) {
                                try {
                                    URL iconUrl = new URL("https:" + weatherData.iconUrl);
                                    ImageIcon icon = new ImageIcon(iconUrl);
                                    weatherIconLabel.setIcon(icon);
                                } catch (Exception ex) {
                                    System.out.println("Error loading icon: " + ex.getMessage());
                                }
                            }
                        } catch (Exception ex) {
                            weatherInfoArea.setText("Error: Unable to fetch weather.");
                        }
                    }
                }.execute();
            } else {
                JOptionPane.showMessageDialog(
                        frame,
                        "City field cannot be empty!",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        });

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // Other methods like createDarkTextField and createDarkButton remain the same

    private static class WeatherData {
        String weatherText;
        String iconUrl;

        WeatherData(String weatherText, String iconUrl) {
            this.weatherText = weatherText;
            this.iconUrl = iconUrl;
        }
    }

    private static WeatherData getWeatherData(String city) {
        StringBuilder result = new StringBuilder();
        String iconUrl = null;
        try {
            // Encode city name and construct full API URL
            String encodedCity = URLEncoder.encode(city, "UTF-8");
            String weatherUrl = API_URL + API_KEY + "&q=" + encodedCity + "&aqi=yes";
            System.out.println(weatherUrl);
            URI uri = new URI(weatherUrl);
            URL url = uri.toURL();

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                Scanner scanner = new Scanner(url.openStream());
                while (scanner.hasNext()) {
                    result.append(scanner.nextLine());
                }
                scanner.close();

                // Parse JSON to get weather text and icon URL
                JSONObject jsonObject = new JSONObject(result.toString());
                String weatherText = parseWeatherData(jsonObject);

                // Get icon URL from JSON
                JSONObject condition = jsonObject.getJSONObject("current").getJSONObject("condition");
                iconUrl = condition.getString("icon");

                return new WeatherData(weatherText, iconUrl);
            } else {
                return new WeatherData("Error: City not found or invalid API request.", null);
            }
        } catch (IOException | URISyntaxException e) {
            return new WeatherData("Error: " + e.getMessage(), null);
        }
    }

    private static String parseWeatherData(JSONObject jsonObject) {
        // Location details
        JSONObject location = jsonObject.getJSONObject("location");
        String cityName = location.getString("name");
        String region = location.getString("region");
        String country = location.getString("country");
        String localTime = location.getString("localtime");

        // Current weather details
        JSONObject current = jsonObject.getJSONObject("current");
        double tempCelsius = current.getDouble("temp_c");
        double tempFahrenheit = current.getDouble("temp_f");
        int humidity = current.getInt("humidity");
        double windSpeed = current.getDouble("wind_kph");
        String windDirection = current.getString("wind_dir");
        double precipitation = current.getDouble("precip_mm");
        String condition = current.getJSONObject("condition").getString("text");

        // Air quality (if available)
        String airQualityWarning = "No specific air quality warnings.";
        if (current.has("air_quality")) {
            JSONObject airQuality = current.getJSONObject("air_quality");
            int usEpaIndex = airQuality.getInt("us-epa-index");

            // Translate EPA index to a warning
            switch (usEpaIndex) {
                case 1:
                    airQualityWarning = "Good air quality";
                    break;
                case 2:
                    airQualityWarning = "Moderate air quality";
                    break;
                case 3:
                    airQualityWarning = "Unhealthy for sensitive groups";
                    break;
                case 4:
                    airQualityWarning = "Unhealthy air quality";
                    break;
                case 5:
                    airQualityWarning = "Very unhealthy air quality";
                    break;
                case 6:
                    airQualityWarning = "Hazardous air quality";
                    break;
                default:
                    airQualityWarning = "Air quality data unavailable";
            }
        }

        // Construct detailed weather report
        return String.format(
                "Location Details:\n" +
                        "City: %s, %s, %s\n" +
                        "Local Time: %s\n\n" +
                        "Weather Conditions:\n" +
                        "Temperature: %.1f °C (%.1f °F)\n" +
                        "Condition: %s\n" +
                        "Humidity: %d%%\n" +
                        "Wind: %.1f km/h %s\n" +
                        "Precipitation: %.1f mm\n\n" +
                        "Air Quality: %s",
                cityName, region, country,
                localTime,
                tempCelsius, tempFahrenheit,
                condition,
                humidity,
                windSpeed, windDirection,
                precipitation,
                airQualityWarning
        );
    }

    private static JButton createDarkButton(String text) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ACCENT_COLOR.darker());
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 15, 15));
                super.paintComponent(g2);
                g2.dispose();
            }
        };
        button.setOpaque(false);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setForeground(DARK_TEXT);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        return button;
    }


    private static JTextField createDarkTextField(String placeholder) {
        JTextField textField = new JTextField(20) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(DARK_SECONDARY_BACKGROUND);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 15, 15));
                super.paintComponent(g2);
                g2.dispose();
            }
        };
        textField.setOpaque(false);
        textField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        textField.setForeground(DARK_TEXT);
        textField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60, 60, 60), 1),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        textField.setCaretColor(DARK_TEXT);
        return textField;
    }


    private static String fetchAlerts(String city) {
        StringBuilder result = new StringBuilder();

        try {
            String encodedCity = URLEncoder.encode(city, "UTF-8");
            String alertsUrl = ALERTS_API_URL + API_KEY + "&q=" + encodedCity;
            System.out.println("-----");
            System.out.println(alertsUrl);

            HttpURLConnection conn = (HttpURLConnection) new URL(alertsUrl).openConnection();
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() == 200) {
                Scanner scanner = new Scanner(conn.getInputStream());
                while (scanner.hasNext()) {
                    result.append(scanner.nextLine());
                }
                scanner.close();

                JSONObject jsonObject = new JSONObject(result.toString());
                return parseJsonSafely(jsonObject.toString());
            } else {
                return "Error fetching alerts.";
            }
        } catch (IOException e) {
            return "Error: " + e.getMessage();
        }
    }

    private static String parseJsonSafely(String jsonString) {
        StringBuilder parsedResult = new StringBuilder();
        try {
            JSONObject jsonObject = new JSONObject(jsonString);

            // Parse the 'location' part
            if (jsonObject.has("location")) {
                parsedResult.append("\n--------------------\nLocation Details:\n");
                JSONObject location = jsonObject.getJSONObject("location");

                parsedResult.append(parseKeyValue(location, "name", "City Name"));
                parsedResult.append(parseKeyValue(location, "region", "Region"));
                parsedResult.append(parseKeyValue(location, "country", "Country"));
                parsedResult.append(parseKeyValue(location, "lat", "Latitude"));
                parsedResult.append(parseKeyValue(location, "lon", "Longitude"));
                parsedResult.append(parseKeyValue(location, "localtime", "Local Time"));
                parsedResult.append("\n");
            }

            // Parse the 'alerts' part
            if (jsonObject.has("alerts")) {
                parsedResult.append("Alerts:\n");
                JSONObject alerts = jsonObject.getJSONObject("alerts");

                if (alerts.has("alert")) {
                    alerts.getJSONArray("alert").forEach(alertObject -> {
                        try {
                            JSONObject alert = (JSONObject) alertObject;
                            parsedResult.append(parseKeyValue(alert, "headline", "Headline"));
                            parsedResult.append(parseKeyValue(alert, "event", "Event"));
                            parsedResult.append(parseKeyValue(alert, "severity", "Severity"));
                            parsedResult.append(parseKeyValue(alert, "urgency", "Urgency"));
                            parsedResult.append(parseKeyValue(alert, "desc", "Description"));
                            parsedResult.append(parseKeyValue(alert, "instruction", "Instructions"));
                            parsedResult.append("\n");
                        } catch (Exception ex) {
                            parsedResult.append("Error parsing alert: ").append(ex.getMessage()).append("\n");
                        }
                    });
                } else {
                    parsedResult.append("No alerts found.\n");
                }
            }

        } catch (Exception e) {
            parsedResult.append("Error parsing JSON: ").append(e.getMessage()).append("\n");
        }

        return parsedResult.toString();
    }

    private static String parseKeyValue(JSONObject object, String key, String displayName) {
        try {
            return String.format("%s: %s\n", displayName, object.get(key).toString());
        } catch (Exception e) {
            return String.format("%s: [Error retrieving key: %s]\n", displayName, key);
        }
    }
}