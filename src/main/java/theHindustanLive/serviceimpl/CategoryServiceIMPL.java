package theHindustanLive.serviceimpl;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;
import theHindustanLive.entity.CategoryEntity;
import theHindustanLive.respository.CategoryRespository;
import theHindustanLive.service.CategoryService;

@AllArgsConstructor
@Service
public class CategoryServiceIMPL implements CategoryService {
	CategoryRespository categoryRespository;

	@Override
	public CategoryEntity categorySave(CategoryEntity categoryEntity) {
		CategoryEntity response = categoryRespository.save(categoryEntity);
		return response;
	}

	@Override
	public List<CategoryEntity> categoryGet() {
		List<CategoryEntity> response = categoryRespository.findAll();
		return response;
	}

	@Override
	public Optional<CategoryEntity> categoryGetById(String id) {
		Optional<CategoryEntity> response = categoryRespository.findById(id);
		return response;
	}

	@Override
	public Optional<CategoryEntity> findById(String id) {
		Optional<CategoryEntity> response = categoryRespository.findById(id);
		return response;
	}

	@Override
	public CategoryEntity savecategory(CategoryEntity categoryEntity) {
		CategoryEntity response = categoryRespository.save(categoryEntity);
		return response;
	}

	@Override
	public String deleteCategoryById(String id) {
	    categoryRespository.deleteById(id);
	    return "Category deleted successfully";
	}

		
	}


