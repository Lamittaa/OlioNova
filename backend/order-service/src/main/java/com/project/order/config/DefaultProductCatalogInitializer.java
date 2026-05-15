package com.project.order.config;

import com.project.order.model.ProductLookup;
import com.project.order.repository.ProductLookupRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DefaultProductCatalogInitializer implements CommandLineRunner {

    private final ProductLookupRepo productRepo;

    @Override
    @Transactional
    public void run(String... args) {
        List<DefaultProduct> products = List.of(
                new DefaultProduct("زيتون للمساهم", "OLIVE", "KG", "0.40", List.of("زيتون للمساهم")),
                new DefaultProduct("زيتون لغير المساهم", "OLIVE", "KG", "0.60", List.of("زيتون لغير المساهم", "زيتون للغير مساهم")),
                new DefaultProduct("الجفت", "JIFT", "PCS", "12.00", List.of("الجفت", "جفت")),
                new DefaultProduct("الجالونات", "GALLON", "PCS", "15.00", List.of("الجالونات", "جالون", "الجالون"))
        );

        for (DefaultProduct defaultProduct : products) {
            ProductLookup product = productRepo
                    .findByProductNameIgnoreCase(defaultProduct.name())
                    .or(() -> defaultProduct.aliases().stream()
                            .map(productRepo::findByProductNameIgnoreCase)
                            .flatMap(java.util.Optional::stream)
                            .findFirst())
                    .orElseGet(ProductLookup::new);

            product.setProductName(defaultProduct.name());
            product.setProductType(defaultProduct.type());
            product.setUnit(defaultProduct.unit());
            product.setPrice(new BigDecimal(defaultProduct.price()));
            product.setInventoryTotalQuantity(null);
            product.setInventoryAvailabilityQuantity(null);
            product.setMemberDiscount(null);
            product.setActive(true);

            productRepo.save(product);

            for (String alias : defaultProduct.aliases()) {
                if (alias.equalsIgnoreCase(defaultProduct.name())) {
                    continue;
                }
                productRepo.findByProductNameIgnoreCase(alias)
                        .filter(aliasProduct -> aliasProduct.getId() != null)
                        .filter(aliasProduct -> product.getId() == null || !aliasProduct.getId().equals(product.getId()))
                        .ifPresent(aliasProduct -> {
                            aliasProduct.setActive(false);
                            productRepo.save(aliasProduct);
                        });
            }
        }

        productRepo.findByProductNameIgnoreCase("\u0639\u0635\u064a\u0631 \u0632\u064a\u062a\u0648\u0646")
                .ifPresent(product -> {
                    product.setActive(false);
                    productRepo.save(product);
                });
    }

    private record DefaultProduct(String name, String type, String unit, String price, List<String> aliases) {
    }
}
