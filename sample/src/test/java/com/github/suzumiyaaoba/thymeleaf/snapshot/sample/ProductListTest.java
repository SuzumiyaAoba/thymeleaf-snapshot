package com.github.suzumiyaaoba.thymeleaf.snapshot.sample;

import com.github.suzumiyaaoba.thymeleaf.snapshot.Snapshot;
import com.github.suzumiyaaoba.thymeleaf.snapshot.SnapshotConfig;
import com.github.suzumiyaaoba.thymeleaf.snapshot.SnapshotTest;
import com.github.suzumiyaaoba.thymeleaf.snapshot.ThymeleafSnapshotExtension;
import java.util.List;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Demonstrates classpath template rendering with a list of POJOs. prettyPrint = true formats the
 * stored snapshot for easier review.
 */
@ExtendWith(ThymeleafSnapshotExtension.class)
@SnapshotConfig(prettyPrint = true)
class ProductListTest {

  @SnapshotTest(template = "product-list")
  void shouldRenderProductList(Snapshot snapshot) {
    var products =
        List.of(
            new Product("Widget Pro", "$49.99", true),
            new Product("Gadget Max", "$129.00", true),
            new Product("Doohickey", "$9.99", false));
    snapshot
        .setVariable("pageTitle", "Our Products")
        .setVariable("products", products)
        .assertMatchesSnapshot();
  }

  @SnapshotTest(template = "product-list")
  void shouldRenderEmptyProductList(Snapshot snapshot) {
    snapshot
        .setVariable("pageTitle", "Our Products")
        .setVariable("products", List.of())
        .assertMatchesSnapshot();
  }

  public static final class Product {
    private final String name;
    private final String price;
    private final boolean available;

    public Product(String name, String price, boolean available) {
      this.name = name;
      this.price = price;
      this.available = available;
    }

    public String getName() {
      return name;
    }

    public String getPrice() {
      return price;
    }

    public boolean isAvailable() {
      return available;
    }
  }
}
