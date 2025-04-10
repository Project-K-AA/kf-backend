package com.kabutar.keyfort.controller.v1;

import java.util.*;

import com.kabutar.keyfort.Entity.Token;
import com.kabutar.keyfort.Entity.User;
import com.kabutar.keyfort.dto.ClientDto;
import com.kabutar.keyfort.dto.TokenDto;
import com.kabutar.keyfort.dto.UserDto;
import com.kabutar.keyfort.service.AuthService;
import com.kabutar.keyfort.service.JwtService;
import com.kabutar.keyfort.util.ResponseHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@RestController
@RequestMapping("/api/v1/auth/{dimension}")
public class AuthController {

	@Autowired
	private AuthService authService;

	@Autowired
	private JwtService jwtService;
	
	private final Logger logger  = LogManager.getLogger(AuthController.class);

	/**
	 *
	 * @param clientId
	 * @param userDto
	 * @return
	 */

	@PostMapping("/login_action")
	public ResponseEntity<?> loginAction(
			@RequestParam String clientId,
			@RequestBody UserDto userDto,
			@PathVariable String dimension
	){
		User user = authService.matchUserCredential(userDto.getUsername(), userDto.getPassword(), clientId, dimension);
		if(user != null){
			Token token = authService.getAuthTokenForUser(user);
			return new ResponseHandler()
					.status(HttpStatus.OK)
					.data(
							List.of( Map.of("authorizationCode",token.getToken()) )
					)
					.build();
		}
		return new ResponseHandler()
				.status(HttpStatus.UNAUTHORIZED)
				.build();
	}

	/**
	 *
	 * @param authorization
	 * @param resourceUrl
	 * @return
	 */
	@GetMapping("/token")
	public ResponseEntity<?> validateToken(
			@RequestHeader(name = "authorization") String authorization,
			@RequestParam String resourceUrl,
			@PathVariable String dimension
	){
		String token = authorization.replaceFirst("^Bearer ","");

		if(authService.validateAccessToken(token,resourceUrl,dimension)){
			return new ResponseHandler()
					.status(HttpStatus.OK)
					.build();
		}

		return new ResponseHandler()
				.status(HttpStatus.UNAUTHORIZED)
				.build();
	}

	/**
	 *
	 * @param tokenDto
	 * @return
	 */
	@PostMapping("/token")
	public ResponseEntity<?> token(
			@RequestBody TokenDto tokenDto,
			@PathVariable String dimension
	){
		try{
			Map<String,Object> tokens = authService.exchangeForTokens(tokenDto.getToken(),tokenDto.getGrantType(), dimension);

			if(!((boolean) tokens.get("isValid"))){
				return new ResponseHandler()
						.status(HttpStatus.UNAUTHORIZED)
						.build();
			}

			return new ResponseHandler()
					.data(List.of(tokens))
					.status(HttpStatus.OK)
					.build();



		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseHandler()
					.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.error(List.of(e.getLocalizedMessage()))
					.build();
		}
    }
	
	@PostMapping("/authz_client")
	public ResponseEntity<?> authorizeClient(
			@RequestBody ClientDto client,
			@PathVariable String dimension
	){

		if(authService.isClientValid(
				client.getClientId(),
				client.getClientSecret(),
				client.getRedirectUri(),
				client.getGrantType(),
				dimension
		)){
			//success
			logger.info("Client with id: {}, requested authorization",client.getClientId());
			return new ResponseHandler()
					.status(HttpStatus.OK)
					.build();
		}

		// all cases failure

		return new ResponseHandler()
				.status(HttpStatus.BAD_REQUEST)
				.error(List.of("Invalid requester details"))
				.build();
	}
}
