package theHindustanLive.service;

import java.util.List;
import java.util.Optional;

import theHindustanLive.entity.CategoryEntity;

public interface CategoryService {

	CategoryEntity categorySave(CategoryEntity categoryEntity);

	List<CategoryEntity> categoryGet();

	Optional<CategoryEntity> categoryGetById(String id);

	Optional<CategoryEntity> findById(String id);

	CategoryEntity savecategory(CategoryEntity categoryEntity);

	String deleteCategoryById(String id);

}
