package id.idtrust.billing.repository;

import id.idtrust.billing.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import javax.persistence.Table;
import java.util.List;

@Repository
@Table(name="product")
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    @Query( value = "SELECT p FROM Product p WHERE pkey = :pkey")
    Product findByPkey(@Param("pkey") String pkey);

    @Query( value = "SELECT p FROM Product p WHERE category = :category")
    List<Product> findByCategory(@Param("category") String category);

    @Query( value = "SELECT p FROM Product p, Account a WHERE p.id = a.product.id and a.xkey = :xkey")
    List<Product> findByAccount(@Param("xkey") String xkey);

    @Query( value = "SELECT p FROM Product p where mitra is true")
    List<Product> fingAllAccountMitraProduct();

    @Query( value = "SELECT p FROM Product p, Account a WHERE p.id = a.product.id and a.xkey = :xkey and mitra is true")
    List<Product> findByAccountMitraProduct(@Param("xkey") String xkey);

    @Query( value = "SELECT p FROM Product p, Account a WHERE p.id = a.product.id and a.xkey = :xkey and p.category= :category")
    List<Product> findByAccountWCategory(@Param("xkey") String xkey, @Param("category") String category);

    @Query( value = "SELECT p FROM Product p WHERE id = :id")
    Product findByID(@Param("id") Long id);
}