package com.receiptprocessor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.UUID;

@SpringBootApplication
@RestController
@RequestMapping("/receipts")
public class ReceiptProcessorApplication {
    private final Map<String, Receipt> receiptStorage = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        SpringApplication.run(ReceiptProcessorApplication.class, args);
    }

    @PostMapping("/process")
    public Map<String, String> processReceipt(@RequestBody Receipt receipt) {
        validateReceipt(receipt);
        String id = UUID.randomUUID().toString();
        receiptStorage.put(id, receipt);
        return Collections.singletonMap("id", id);
    }

    @GetMapping("/{id}/points")
    public Map<String, Integer> getPoints(@PathVariable String id) {
        Receipt receipt = receiptStorage.get(id);
        if (receipt == null) {
            throw new NoSuchElementException("Receipt not found");
        }
        return Collections.singletonMap("points", calculatePoints(receipt));
    }

    private int calculatePoints(Receipt receipt) {
        int points = 0;
        
        // 1. One point per alphanumeric character in the retailer name
        points += receipt.getRetailer().replaceAll("[^a-zA-Z0-9]", "").length();

        // 2. Using BigDecimal for accurate currency calculations
        BigDecimal total = new BigDecimal(receipt.getTotal());

        // 3. 50 points if the total is a whole number
        if (total.scale() == 0) {
            points += 50;
        }

        // 4. 25 points if the total is a multiple of 0.25
        if (total.remainder(BigDecimal.valueOf(0.25)).compareTo(BigDecimal.ZERO) == 0) {
            points += 25;
        }

        // 5. 5 points for every two items
        points += (receipt.getItems().size() / 2) * 5;

        // 6. Item description bonus (20% of item price if description length is a multiple of 3)
        for (ReceiptItem item : receipt.getItems()) {
            String trimmedDesc = item.getShortDescription().trim();
            if (trimmedDesc.length() % 3 == 0) {
                BigDecimal price = new BigDecimal(item.getPrice());
                points += price.multiply(BigDecimal.valueOf(0.2)).setScale(0, RoundingMode.CEILING).intValue();
            }
        }

        // 7. 6 points if the purchase day is odd
        int day = LocalDate.parse(receipt.getPurchaseDate()).getDayOfMonth();
        if (day % 2 == 1) {
            points += 6;
        }

        // 8. 10 points if the purchase time is between 2:00 PM and 4:00 PM
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalTime time = LocalTime.parse(receipt.getPurchaseTime(), timeFormatter);
        if (time.isAfter(LocalTime.of(14, 0)) && time.isBefore(LocalTime.of(16, 0))) {
            points += 10;
        }

        return points;
    }

    private void validateReceipt(Receipt receipt) {
        if (receipt.getRetailer() == null || receipt.getRetailer().isEmpty()) {
            throw new IllegalArgumentException("Retailer name is required");
        }

        // Validate date format and ensure it's a real date
        try {
            LocalDate.parse(receipt.getPurchaseDate(), DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid purchase date format. Expected YYYY-MM-DD");
        }

        // Validate time format using DateTimeFormatter
        try {
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
            LocalTime.parse(receipt.getPurchaseTime(), timeFormatter);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid purchase time format. Expected HH:MM");
        }
    }
}

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Collections.singletonMap("error", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Collections.singletonMap("error", e.getMessage()));
    }
}

class Receipt {
    private String retailer;
    private String purchaseDate;
    private String purchaseTime;
    private List<ReceiptItem> items;
    private String total;

    public String getRetailer() 
{ 
    return retailer; 
}
    public String getPurchaseDate() 
{ 
    return purchaseDate;
}
    public String getPurchaseTime() 
{ 
    return purchaseTime; 
}
    public List<ReceiptItem> getItems() 
{   
    return items; 
}
    public String getTotal() 
{   
    return total; 
}
}

class ReceiptItem {
    private String shortDescription;
    private String price;

    public String getShortDescription() { return shortDescription; }
    public String getPrice() { 
    return price;
}
}
