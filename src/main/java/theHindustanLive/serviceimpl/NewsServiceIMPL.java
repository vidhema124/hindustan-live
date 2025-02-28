package theHindustanLive.serviceimpl;

import java.io.StringReader;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import lombok.AllArgsConstructor;
import theHindustanLive.config.DateTimeUtil;
import theHindustanLive.entity.NewsEntity;
import theHindustanLive.respository.NewRespository;
import theHindustanLive.service.NewsService;

@AllArgsConstructor
@Service
public class NewsServiceIMPL implements NewsService {
	NewRespository newRespository;

	
	@Autowired
	private MongoTemplate mongoTemplate;

	private static final Map<String, String> RSS_URLS = new HashMap<>();
	private static final Map<String, String> PUBLISHER_ICONS = new HashMap<>();

	static {
	
	RSS_URLS.put("https://www.bhaskar.com/rss-v1--category-1053.xml", "Sports");
    RSS_URLS.put("https://www.thehindu.com/sport/feeder/default.rss", "Sports");
    RSS_URLS.put("https://arynews.tv/category/sports/feed/", "Sports");  
   
    RSS_URLS.put("https://cms.patrika.com/blog/category/health-news/feed/", "Health");
    RSS_URLS.put("https://www.thehindu.com/sci-tech/health/feeder/default.rss", "Health");
    RSS_URLS.put("https://feeds.nbcnews.com/nbcnews/public/health", "Health"); 
    
    RSS_URLS.put("https://www.bhaskar.com/rss-v1--category-1051.xml", "Business");
    RSS_URLS.put("https://www.thehindu.com/business/feeder/default.rss", "Business");
    RSS_URLS.put("https://arynews.tv/category/business/feed/", "Business");
    
    RSS_URLS.put("https://feeds.science.org/rss/science.xml", "Science");
    RSS_URLS.put("https://www.thehindu.com/sci-tech/science/feeder/default.rss", "Science");
    RSS_URLS.put("https://www.livemint.com/rss/science", "Science");
    
    RSS_URLS.put("https://www.bhaskar.com/rss-v1--category-5707.xml", "Technology");
    RSS_URLS.put("https://arynews.tv/category/sci-techno/feed/", "Technology");
    
    RSS_URLS.put("https://www.bhaskar.com/rss-v1--category-1125.xml", "World");
    RSS_URLS.put("https://feeds.nbcnews.com/nbcnews/public/news", "World");
    RSS_URLS.put("https://www.livemint.com/rss/news.xml", "World");
    
    RSS_URLS.put("https://www.bhaskar.com/rss-v1--category-1061.xml", "India");
    RSS_URLS.put("https://timesofindia.indiatimes.com/rssfeeds/-2128936835.cms", "India");
    RSS_URLS.put("https://timesofindia.indiatimes.com/rssfeeds/-2128838597.cms", "India");
    
    RSS_URLS.put("https://www.bhaskar.com/rss-v1--category-1740.xml", "Local");
    RSS_URLS.put("https://timesofindia.indiatimes.com/rssfeeds/3012544.cms", "Local");
    RSS_URLS.put("https://timesofindia.indiatimes.com/rssfeeds/-2128839596.cms", "Local");
    
    RSS_URLS.put("https://www.bhaskar.com/rss-v1--category-11215.xml", "Entertainment");
    RSS_URLS.put("https://timesofindia.indiatimes.com/rssfeeds/2886704.cms", "Entertainment");
    
   
    PUBLISHER_ICONS.put("The Hindu", "https://vfic.tamu.edu/files/2014/03/LogoThe-Hindu2.png");
    PUBLISHER_ICONS.put("NBC News", "https://upload.wikimedia.org/wikipedia/commons/9/97/NBC_News_logo.png");
    PUBLISHER_ICONS.put("Timesofindia", "https://logodix.com/logo/1113831.jpg");
    PUBLISHER_ICONS.put("Livemint News", "https://tse4.mm.bing.net/th?id=OIP.xMcKwuoYnhP2UBBCt0WdKwHaBv&pid=Api&P=0&h=180");
    PUBLISHER_ICONS.put("Ary News",
			"https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQVRrQiEXpTEncBl1iP09qPYf6P88YR7d1Chw&s");
    PUBLISHER_ICONS.put("AAAS News","https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSy61zAinBEKbPgFliUa9eU_YmJUm-rE8W_Kw&s");
    PUBLISHER_ICONS.put("The Indian Express",
			"https://upload.wikimedia.org/wikipedia/commons/thumb/2/24/The_Indian_Express_logo.svg/606px-The_Indian_Express_logo.svg.png");
    PUBLISHER_ICONS.put("Danik Bhaskar",
			"https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRFSaPn1bxgi0F9iAfHYl4Q79Q42VUPa4wT-A&s");
    PUBLISHER_ICONS.put("ABP News",
			"https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTomfFMcDUU8umzGHL5nksmsjvDeJYXl66wuQ&s");
    PUBLISHER_ICONS.put("Patrika News",
			"https://www.patrika.com/images/patrika-logo.jpg");
    

}

	@Override
	public List<NewsEntity> fetchNewsFromRSS(List<String> categoryFilters) {
	    Map<String, NewsEntity> combinedNewsMap = new HashMap<>();

	    try {
	        for (Map.Entry<String, String> entry : RSS_URLS.entrySet()) {
	            String urlString = entry.getKey();
	            String category = entry.getValue();

	            if (categoryFilters != null && !categoryFilters.isEmpty() && !categoryFilters.contains(category)) {
	                continue;
	            }

				URL url = new URL(urlString);
				RestTemplate restTemplate = new RestTemplate();
				String xmlData = restTemplate.getForObject(url.toURI(), String.class);

				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				DocumentBuilder builder = factory.newDocumentBuilder();
				InputSource is = new InputSource(new StringReader(xmlData));
				org.w3c.dom.Document doc = builder.parse(is);
				NodeList nodeList = doc.getElementsByTagName("item");

				int limit = Math.min(nodeList.getLength(), 5);

				for (int i = 0; i < limit; i++) {
					Element element = (Element) nodeList.item(i);

					String title = element.getElementsByTagName("title").item(0).getTextContent();
					String description = element.getElementsByTagName("description").item(0).getTextContent();
					String link = element.getElementsByTagName("link").item(0).getTextContent();
					String pubDateStr = element.getElementsByTagName("pubDate").item(0).getTextContent();

					// Parsing pubDate from RSS
					LocalDateTime pubDateLocalDateTime = DateTimeUtil.parsePubDate(pubDateStr);
					Date pubDate = Date.from(pubDateLocalDateTime.atZone(ZoneId.systemDefault()).toInstant());

					String imageUrl = extractImageUrl(description);
					NodeList mediaList = element.getElementsByTagName("media:content");
					if (mediaList.getLength() > 0) {
						Element media = (Element) mediaList.item(0);
						String mediaImageUrl = media.getAttribute("url");
						if (mediaImageUrl != null && !mediaImageUrl.isEmpty()) {
							imageUrl = mediaImageUrl;
						}
						
					}
					
					String publisher = getPublisherFromRSS(urlString);
					String publisherIcon = getPublisherIcon(publisher)
;
					if (publisherIcon == null || !isValidURL(publisherIcon)) {
						publisherIcon = "https://via.placeholder.com/150?text=No+Logo";
					}

					NewsEntity existingNews = newRespository.findByTitle(title);
					if (existingNews != null) {
						if (!existingNews.getPublisher().contains(publisher)) {
							existingNews.setPublisher(existingNews.getPublisher() + ", " + publisher);
							existingNews.setPublisherIcon(existingNews.getPublisherIcon() + ", " + publisherIcon);
						}
						existingNews.setUpdatedAt(new Date());
						newRespository.save(existingNews);
						continue;
					}

					NewsEntity news = new NewsEntity();
					news.setTitle(title);
					news.setDescription(description);
					news.setLink(link)
;
					news.setPubDate(pubDate); // Set parsed LocalDateTime directly?
					news.setImageUrl(imageUrl);
					news.setPublisher(publisher)
;
					news.setPublisherIcon(publisherIcon);
					news.setCategory(category);
					news.setCreatedAt(new Date());
					news.setUpdatedAt(new Date());
					news.generateSlug();

					combinedNewsMap.put(title, news);
					newRespository.save(news);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new ArrayList<>(combinedNewsMap.values());
	}

	private String extractImageUrl(String description) {
		if (description == null || description.isEmpty()) {
			return null;
		}
		try {
			Pattern pattern = Pattern.compile("<img[^>]*src=\"([^\"]*)\"");
			Matcher matcher = pattern.matcher(description);
			if (matcher.find()) {
				return matcher.group(1);
			}
		} catch (Exception e) {
			System.err.println("Error extracting image URL: " + e.getMessage());
		}
		return null;
	}

	private String getPublisherFromRSS(String url) {
    if (url.contains("thehindu")) {
        return "The Hindu";
    } else if (url.contains("nbcnews")) {
        return "NBC News";
    } else if (url.contains("timesofindia")) {
        return "Timesofindia";
    } else if (url.contains("livemint")) {
        return "Livemint News";
    } else if (url.contains("arynews")) {
        return "Ary News";
    } else if (url.contains("science")) {
        return "AAAS News";
    }else if (url.contains("indianexpress")) {
		return "The Indian Express";
		}
    else if (url.contains("bhaskar")) {
  		return "Danik Bhaskar";
  		}
    else if (url.contains("abplive")) {
  		return "ABP News";
  		}
    else if (url.contains("patrika")) {
  		return "Patrika News";
  		}
    return "Unknown Publisher";
}

	private String getPublisherIcon(String publisher) {
		return PUBLISHER_ICONS.getOrDefault(publisher, "https://via.placeholder.com/150");
	}

	private boolean isValidURL(String url) {
		try {
			URL validatedUrl = new URL(url);
			validatedUrl.toURI();
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	
	
	
	
	
	
//	@Autowired
//	private MongoTemplate mongoTemplate;
//
//	private static final Map<String, String> RSS_URLS = new HashMap<>();
//	private static final Map<String, String> PUBLISHER_ICONS = new HashMap<>();
//
//	static {
//		
//		RSS_URLS.put("https://www.thehindu.com/sport/feeder/default.rss", "Sports");
//		RSS_URLS.put("https://www.thehindu.com/sci-tech/health/feeder/default.rss", "Health");
//		RSS_URLS.put("https://feeds.nbcnews.com/nbcnews/public/news", "World");
//		RSS_URLS.put("https://www.thehindu.com/business/feeder/default.rss", "Business");
//		RSS_URLS.put("https://lankanewsweb.net/archives/category/news/feed/", "Politics");
//		RSS_URLS.put("https://www.thehindu.com/sci-tech/science/feeder/default.rss", "Science");
//		RSS_URLS.put("https://www.livemint.com/rss/news.xml", "Technology");
//		RSS_URLS.put("https://timesofindia.indiatimes.com/rssfeeds/-2128936835.cms", "India");
//		RSS_URLS.put("https://timesofindia.indiatimes.com/rssfeeds/3012544.cms", "Local");
//		RSS_URLS.put("https://timesofindia.indiatimes.com/rssfeeds/2886704.cms", "Entertainment");
//
//		PUBLISHER_ICONS.put("The Hindu", "https://vfic.tamu.edu/files/2014/03/LogoThe-Hindu2.png");
//		PUBLISHER_ICONS.put("Livemint News",
//				"https://tse4.mm.bing.net/th?id=OIP.xMcKwuoYnhP2UBBCt0WdKwHaBv&pid=Api&P=0&h=180");
//		PUBLISHER_ICONS.put("Ary News",
//				"https://i.vimeocdn.com/channel/324633_980?mh=250&sig=6ad418f288f7e4319389389a03ebb3a16c0ad00299a5f60a626f9cd2f0c1d736&v=1");
//
//		PUBLISHER_ICONS.put("NBC News", "https://upload.wikimedia.org/wikipedia/commons/9/97/NBC_News_logo.png");
//		PUBLISHER_ICONS.put("Timesofindia", "https://logodix.com/logo/1113831.jpg");
//		PUBLISHER_ICONS.put("The Indian Express",
//				"https://upload.wikimedia.org/wikipedia/commons/thumb/2/24/The_Indian_Express_logo.svg/606px-The_Indian_Express_logo.svg.png");
//	}
//
//	@Override
//	public List<NewsEntity> fetchNewsFromRSS(String categoryFilter) {
//		Map<String, NewsEntity> combinedNewsMap = new HashMap<>();
//
//		try {
//			for (Map.Entry<String, String> entry : RSS_URLS.entrySet()) {
//				String urlString = entry.getKey();
//				String category = entry.getValue();
//
//				if (categoryFilter != null && !category.equalsIgnoreCase(categoryFilter)) {
//					continue;
//				}
//
//				URL url = new URL(urlString);
//				RestTemplate restTemplate = new RestTemplate();
//				String xmlData = restTemplate.getForObject(url.toURI(), String.class);
//
//				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//				DocumentBuilder builder = factory.newDocumentBuilder();
//				InputSource is = new InputSource(new StringReader(xmlData));
//				org.w3c.dom.Document doc = builder.parse(is);
//				NodeList nodeList = doc.getElementsByTagName("item");
//
//				int limit = Math.min(nodeList.getLength(), 10);
//
//				for (int i = 0; i < limit; i++) {
//					Element element = (Element) nodeList.item(i);
//
//					String title = element.getElementsByTagName("title").item(0).getTextContent();
//					String description = element.getElementsByTagName("description").item(0).getTextContent();
//					String link = element.getElementsByTagName("link").item(0).getTextContent();
//					String pubDateStr = element.getElementsByTagName("pubDate").item(0).getTextContent();
//
//					// Parsing pubDate from RSS
//					LocalDateTime pubDateLocalDateTime = DateTimeUtil.parsePubDate(pubDateStr);
//					Date pubDate = Date.from(pubDateLocalDateTime.atZone(ZoneId.systemDefault()).toInstant());
//
//					String imageUrl = extractImageUrl(description);
//					NodeList mediaList = element.getElementsByTagName("media:content");
//					if (mediaList.getLength() > 0) {
//						Element media = (Element) mediaList.item(0);
//						String mediaImageUrl = media.getAttribute("url");
//						if (mediaImageUrl != null && !mediaImageUrl.isEmpty()) {
//							imageUrl = mediaImageUrl;
//						}
//					}
//
//					String publisher = getPublisherFromRSS(urlString);
//					String publisherIcon = getPublisherIcon(publisher);
//					if (publisherIcon == null || !isValidURL(publisherIcon)) {
//						publisherIcon = "https://via.placeholder.com/150?text=No+Logo";
//					}
//
//					NewsEntity existingNews = newRespository.findByTitle(title);
//					if (existingNews != null) {
//						if (!existingNews.getPublisher().contains(publisher)) {
//							existingNews.setPublisher(existingNews.getPublisher() + ", " + publisher);
//							existingNews.setPublisherIcon(existingNews.getPublisherIcon() + ", " + publisherIcon);
//						}
//						existingNews.setUpdatedAt(new Date());
//						newRespository.save(existingNews);
//						continue;
//					}
//
//					NewsEntity news = new NewsEntity();
//					news.setTitle(title);
//					news.setDescription(description);
//					news.setLink(link);
//					news.setPubDate(pubDate); // Set parsed LocalDateTime directly?
//					news.setImageUrl(imageUrl);
//					news.setPublisher(publisher);
//					news.setPublisherIcon(publisherIcon);
//					news.setCategory(category);
//					news.setCreatedAt(new Date());
//					news.setUpdatedAt(new Date());
//					news.generateSlug();
//
//					combinedNewsMap.put(title, news);
//					newRespository.save(news);
//				}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		return new ArrayList<>(combinedNewsMap.values());
//	}
//
//	private String extractImageUrl(String description) {
//		if (description == null || description.isEmpty()) {
//			return null;
//		}
//		try {
//			Pattern pattern = Pattern.compile("<img[^>]*src=\"([^\"]*)\"");
//			Matcher matcher = pattern.matcher(description);
//			if (matcher.find()) {
//				return matcher.group(1);
//			}
//		} catch (Exception e) {
//			System.err.println("Error extracting image URL: " + e.getMessage());
//		}
//		return null;
//	}
//
//	private String getPublisherFromRSS(String url) {
//		if (url.contains("thehindu")) {
//			return "The Hindu";
//		} else if (url.contains("indianexpress")) {
//			return "The Indian Express";
//		} else if (url.contains("timesofindia")) {
//			return "Timesofindia";
//		} else if (url.contains("nbcnews")) {
//			return "NBC News";
//		} else if (url.contains("livemint")) {
//			return "Livemint News";
//		} else if (url.contains("arynews")) {
//			return "Ary News";
//		}
//		return "Unknown Publisher";
//	}
//
//	private String getPublisherIcon(String publisher) {
//		return PUBLISHER_ICONS.getOrDefault(publisher, "https://via.placeholder.com/150");
//	}
//
//	private boolean isValidURL(String url) {
//		try {
//			URL validatedUrl = new URL(url);
//			validatedUrl.toURI();
//			return true;
//		} catch (Exception e) {
//			return false;
//		}
//	}

	@Override
	public void saveNews(List<NewsEntity> newsEntities) {
		newRespository.saveAll(newsEntities);
	}

	@Override
	public String deleteNewsById(String id) {

		if (!newRespository.existsById(id)) {
			return "ID does not exist";
		}

		newRespository.deleteById(id);
		return "News deleted successfully";
	}

	public boolean existsById(String id) {
		return newRespository.existsById(id);
	}

	@Override
	public Optional<NewsEntity> newsGetById(String id) {
		Optional<NewsEntity> response = newRespository.findById(id);
		return response;
	}

	@Override
	public Optional<NewsEntity> findById(String id) {
		Optional<NewsEntity> response = newRespository.findById(id);
		return response;
	}

	@Override
	public NewsEntity saveNews(NewsEntity newsEntity) {
		NewsEntity response = newRespository.save(newsEntity);
		return response;

	}

	@Override
	public List<NewsEntity> findRandomNewsWithImage() {
		return newRespository.findRandomNewsWithImage();
	}

	@Override
	public List<NewsEntity> getNewsByCategory(String category) {
		return newRespository.findByCategory(category);
	}

	@Override
	public Map<String, List<Map<String, Object>>> getNewsByCategory() {
		List<NewsEntity> allNews = newRespository.findAll();

		List<NewsEntity> filteredNews = allNews.stream()
				.filter(news -> news.getImageUrl() != null && !news.getImageUrl().isEmpty())
				.collect(Collectors.toList());

		filteredNews.sort(Comparator.comparing(NewsEntity::getPubDate).reversed());

		Map<String, List<Map<String, Object>>> groupedNews = new HashMap<>();

		
		for (NewsEntity news : filteredNews) {
			String category = news.getCategory();
			Map<String, Object> newsMap = new HashMap<>();

			
			newsMap.put("_id", news.getId());
			newsMap.put("title", news.getTitle());
			newsMap.put("description", news.getDescription());
			newsMap.put("link", news.getLink());
			newsMap.put("pubDate", news.getPubDate());
			newsMap.put("publisher", news.getPublisher());
			newsMap.put("category", news.getCategory());
			newsMap.put("imageUrl", news.getImageUrl());
			newsMap.put("publisherIcon", news.getPublisherIcon());
			newsMap.put("updatedAt", news.getUpdatedAt());
			newsMap.put("createdAt", news.getCreatedAt());
			newsMap.put("slug", news.getSlug());

			groupedNews.computeIfAbsent(category, k -> new ArrayList<>()).add(newsMap);
		}

		groupedNews.forEach((category, newsList) -> {
			if (newsList.size() > 7) {
				groupedNews.put(category, newsList.stream().limit(7).collect(Collectors.toList()));
			}
		});

		return groupedNews;
	}

	@Override
	public List<NewsEntity> getNewsByTitle(String title) {
		String regex = "(?i).*" + title + ".*";
		return newRespository.findByTitleRegexWithImageUrl(regex);
	}

	@Override
	public Map<String, Object> getAllNews(Integer pageNumber, Integer pageSize) {
		pageSize = 10;
		Pageable pageable = PageRequest.of(pageNumber - 1, pageSize,
				Sort.by(Sort.Order.desc("pubDate"), Sort.Order.desc("updatedAt")));
		Page<NewsEntity> pageResponse = newRespository.findAll(pageable);
		List<NewsEntity> newsList = new ArrayList<>(pageResponse.getContent());
		Map<String, Object> response = new HashMap<>();
		response.put("news", newsList);
		response.put("totalPages", pageResponse.getTotalPages());
		response.put("currentPage", pageResponse.getNumber() + 1);

		return response;
	}

	@Override
	public List<NewsEntity> getAllNews() {
		List<NewsEntity> response = newRespository.findAll();
		return response;
	}

	@Override
	@Cacheable(value = "newsCache", key = "#slug", unless = "#result == null")
	public NewsEntity getNewsBySlug(String slug) {
		return newRespository.findBySlug(slug);
	}

	@CacheEvict(value = "newsCache", allEntries = true)
	public void clearCache() {
		System.out.println("Cache cleared!");
	}
}