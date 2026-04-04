import java.util.HashMap;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        // 1. Tạo đồ điện tử
        Map<String, Object> laptopSpecs = new HashMap<>();
        laptopSpecs.put("warranty", 12);
        Item laptop = CreateItem.createItem("electronics", "E01", "MacBook", 2000.0, laptopSpecs);

        // 2. Tạo tác phẩm nghệ thuật
        Map<String, Object> artSpecs = new HashMap<>();
        artSpecs.put("artist", "Picasso");
        Item painting = CreateItem.createItem("art", "A01", "Guernica", 1000000.0, artSpecs);

        laptop.displayInfo();
        painting.displayInfo();
    }
}