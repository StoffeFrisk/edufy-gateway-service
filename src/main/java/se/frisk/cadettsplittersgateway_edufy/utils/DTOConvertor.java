package se.frisk.cadettsplittersgateway_edufy.utils;

import se.frisk.cadettsplittersgateway_edufy.dtos.UserDTO;
import se.frisk.cadettsplittersgateway_edufy.dtos.UserRepresentation;

public class DTOConvertor {

        public static UserDTO toKeycloakDTO(UserRepresentation userRep) {
            UserDTO dto = new UserDTO();
            dto.setKeycloakId(userRep.getId());
            dto.setUsername(userRep.getUsername());
            dto.setFirstName(userRep.getFirstName());
            dto.setLastName(userRep.getLastName());
            dto.setEmail(userRep.getEmail());
            return dto;
        }

        public static UserRepresentation fromUserDTO(UserDTO dto) {
            UserRepresentation userRep = new UserRepresentation();
            userRep.setId(dto.getKeycloakId());
            userRep.setUsername(dto.getUsername());
            userRep.setFirstName(dto.getFirstName());
            userRep.setLastName(dto.getLastName());
            userRep.setEmail(dto.getEmail());
            userRep.setEnabled(true);
            userRep.setEmailVerified(true);
            return userRep;
        }
    }


