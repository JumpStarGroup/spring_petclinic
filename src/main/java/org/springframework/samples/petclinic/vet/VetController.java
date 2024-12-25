/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.vet;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Ken Krebs
 * @author Arjen Poutsma
 */
@Controller
class VetController {

	private final VetRepository vetRepository;

	@Autowired
	public VetController(VetRepository vetRepository) {
		this.vetRepository = vetRepository;
	}

	@GetMapping("/vets.html")
	public String showVetList(@RequestParam(defaultValue = "1") int page, 
							 @RequestParam(required = false) String search,
							 Model model) {
		Vets vets = new Vets();
		Page<Vet> paginated;
		
		if (search != null && !search.trim().isEmpty()) {
			paginated = findPaginatedBySearch(page, search.trim());
			model.addAttribute("search", search);
		} else {
			paginated = findPaginated(page);
		}
		
		vets.getVetList().addAll(paginated.toList());
		return addPaginationModel(page, paginated, model);
	}

	private Page<Vet> findPaginatedBySearch(int page, String search) {
		int pageSize = 5;
		Pageable pageable = PageRequest.of(page - 1, pageSize);
		return vetRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
			search, search, pageable);
	}

	private String addPaginationModel(int page, Page<Vet> paginated, Model model) {
		List<Vet> listVets = paginated.getContent();
		model.addAttribute("currentPage", page);
		model.addAttribute("totalPages", paginated.getTotalPages());
		model.addAttribute("totalItems", paginated.getTotalElements());
		model.addAttribute("listVets", listVets);
		return "vets/vetList";
	}

	private Page<Vet> findPaginated(int page) {
		int pageSize = 5;
		Pageable pageable = PageRequest.of(page - 1, pageSize);
		return vetRepository.findAll(pageable);
	}

	@GetMapping("/vets")
	public @ResponseBody Vets showResourcesVetList() {
		// Here we are returning an object of type 'Vets' rather than a collection of Vet
		// objects so it is simpler for JSon/Object mapping
		Vets vets = new Vets();
		vets.getVetList().addAll(this.vetRepository.findAll());
		return vets;
	}

	@GetMapping("/vets/search")
	public String initFindForm(Model model) {
		model.addAttribute("vet", new Vet());
		return "vets/findVets";
	}

	@GetMapping("/vets/search-results")
	public String processFindForm(
		Vet vet, BindingResult result, @RequestParam(defaultValue = "1") int page, Model model
	) {
		// Validate search criteria
		if (isEmptySearch(vet)) {
			result.rejectValue("lastName", "notFound", "Please enter a name to search");
			return "vets/findVets";
		}

		Page<Vet> vetsResults = findPaginatedForVetsName(page, vet.getFirstName(), vet.getLastName());
		if (vetsResults.isEmpty()) {
			result.rejectValue("lastName", "notFound", "No vets found matching your criteria");
			return "vets/findVets";
		}

		return addPaginationModel(page, vetsResults, model);
	}

	private boolean isEmptySearch(Vet vet) {
		return (vet.getFirstName() == null || vet.getFirstName().isEmpty()) &&
			   (vet.getLastName() == null || vet.getLastName().isEmpty());
	}

	private Page<Vet> findPaginatedForVetsName(int page, String firstName, String lastName) {
		int pageSize = 5;
		Pageable pageable = PageRequest.of(page - 1, pageSize);
		return vetRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
			firstName, lastName, pageable
		);
	}
}
