package com.example.backend.dto.character;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CharacterCreateRequest {

    @NotBlank(message = "캐릭터 이름은 필수 입력 항목입니다.")
    @Size(min = 2, max = 20, message = "캐릭터 이름은 2자 이상 20자 이하로 입력해주세요.")
    @Pattern(regexp = "^[가-힣a-zA-Z0-9_-]+$",
            message = "캐릭터 이름은 한글, 영문, 숫자, 언더스코어(_), 하이픈(-)만 사용 가능합니다.")
    private String charName;
}