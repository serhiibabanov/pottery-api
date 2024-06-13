package com.pottery.service.products.repositories.impl;

import com.pottery.service.products.dtos.ProductShortDto;
import com.pottery.service.products.dtos.QProductShortDto;
import com.pottery.service.products.entities.Product;
import com.pottery.service.products.entities.QProduct;
import com.pottery.service.products.repositories.QueryDslProductRepository;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class QueryDslProductRepositoryImpl implements QueryDslProductRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<ProductShortDto> getProductViews(List<Long> categoryIds,
                                                 List<Long> colorIds,
                                                 List<Long> collectionIds,
                                                 BigDecimal minPrice, BigDecimal maxPrice,
                                                 Boolean isAvailable,
                                                 String sort,
                                                 Pageable pageable) {
        QProduct product = new QProduct("product");

        BooleanExpression[] expressions = {
                categoryIdIn(categoryIds),
                colorIdIn(colorIds),
                collectionIdIn(collectionIds),
                minPrice(minPrice),
                maxPrice(maxPrice),
                isAvailable(isAvailable)
        };

        JPAQuery<ProductShortDto> query = queryFactory
                .select(
                        new QProductShortDto(
                                product.id,
                                product.name,
                                product.images,
                                product.catalogPrice,
                                product.discountCatalogPrice
                        )
                )
                .from(product)
                .where(
                        expressions
                )
                .orderBy(getSortedColumn(pageable));

        List<ProductShortDto> products = query.fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(product.count())
                .from(product)
                .where(
                        expressions
                );

        return PageableExecutionUtils.getPage(products, pageable,
                countQuery::fetchOne);

    }

    private BooleanExpression categoryIdIn(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return null;
        }

        QProduct product = new QProduct("product");
        return product.category.id.in(categoryIds);
    }

    private BooleanExpression colorIdIn(List<Long> colorIds) {
        if (colorIds == null || colorIds.isEmpty()) {
            return null;
        }

        QProduct product = new QProduct("product");
        return product.properties.any().color.id.in(colorIds);
    }

    private BooleanExpression collectionIdIn(List<Long> collectionIds) {
        if (collectionIds == null || collectionIds.isEmpty()) {
            return null;
        }

        QProduct product = new QProduct("product");
        return product.collections.any().id.in(collectionIds);
    }

    private BooleanExpression minPrice(BigDecimal minPrice) {
        if (minPrice == null) {
            return null;
        }

        QProduct product = new QProduct("product");
        return product.discountCatalogPrice.isNotNull().and(product.discountCatalogPrice.goe(minPrice))
                .or(product.discountCatalogPrice.isNull().and(product.catalogPrice.goe(minPrice)));
    }

    private BooleanExpression maxPrice(BigDecimal maxPrice) {
        if (maxPrice == null) {
            return null;
        }

        QProduct product = new QProduct("product");
        return product.discountCatalogPrice.isNotNull().and(product.discountCatalogPrice.loe(maxPrice))
                .or(product.discountCatalogPrice.isNull().and(product.catalogPrice.loe(maxPrice)));
    }

    private BooleanExpression isAvailable(boolean isAvailable) {
        if (!isAvailable) {
            return null;
        }

        QProduct product = new QProduct("product");
        return product.properties.any().quantity.gt(0);
    }

    private OrderSpecifier<?>[] getSortedColumn(Pageable pageable) {
        return pageable.getSort().stream()
                .map(o -> {
                    PathBuilder<Product> orderByExpression = new PathBuilder<>(Product.class, "product");

                    return new OrderSpecifier(o.isAscending() ? com.querydsl.core.types.Order.ASC
                            : com.querydsl.core.types.Order.DESC, orderByExpression.get(o.getProperty()));
                })
                .toArray(OrderSpecifier[]::new);
    }

}