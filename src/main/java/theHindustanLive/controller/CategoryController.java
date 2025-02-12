package theHindustanLive.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.AllArgsConstructor;
import theHindustanLive.entity.CategoryEntity;
import theHindustanLive.entity.NewsEntity;
import theHindustanLive.service.CategoryService;

@AllArgsConstructor
@RestController
@RequestMapping("/news")
@CrossOrigin(origins = "http://localhost:3000")
public class CategoryController {
	CategoryService categoryService;

	@PostMapping("/category-save")
	public ResponseEntity<CategoryEntity> saveCategory(@RequestBody CategoryEntity categoryEntity) {
		CategoryEntity response = categoryService.categorySave(categoryEntity);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/getAll-category")
	public ResponseEntity<List<CategoryEntity>> getCategory() {
		List<CategoryEntity> response = categoryService.categoryGet();
		return ResponseEntity.ok(response);
	}

	@GetMapping("/get-categoryById/{id}")
	public Optional<CategoryEntity> CategoryId(@PathVariable String id) {
		Optional<CategoryEntity> response = categoryService.categoryGetById(id);
		return response;
	}

	@PutMapping("/updateCategoryById/{id}")
	public String updateById(@PathVariable("id") String id, @RequestBody CategoryEntity categoryEntity) {
		Optional<CategoryEntity> emp = categoryService.findById(id);
		if (emp.isPresent()) {
			categoryEntity.setId(id);
			categoryService.savecategory(categoryEntity);
			return "Product Details updated";
		} else {
			return "Product details do not exist";
		}
	}

	@DeleteMapping("/delete-category/{id}")
	public ResponseEntity<Map<String, String>> deleteNews(@PathVariable String id) {
		String result = categoryService.deleteCategoryById(id);
		Map<String, String> response = new HashMap<>();
		response.put("status", "success");
		response.put("message", result);
		return ResponseEntity.ok(response);
	}

}
