package com.example.backend.config;

import com.example.backend.entity.Station;
import com.example.backend.entity.Story;
import com.example.backend.entity.Page;
import com.example.backend.entity.Options;
import com.example.backend.repository.StationRepository;
import com.example.backend.repository.StoryRepository;
import com.example.backend.repository.PageRepository;
import com.example.backend.repository.OptionsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final StationRepository stationRepository;
    private final StoryRepository storyRepository;
    private final PageRepository pageRepository;
    private final OptionsRepository optionsRepository;

    @Override
    @Transactional
    public void run(String... args) {
        initializeStations();
        initializeStoryData();
    }

    private void initializeStations() {
        if (stationRepository.count() > 0) {
            log.info("이미 데이터가 존재합니다. 초기화를 건너뜁니다. ({}개)", stationRepository.count());
            return;
        }

        List<Station> stations = Arrays.asList(
                createStation("1001000113", "도봉산", 1),
                createStation("1001000114", "도봉", 1),
                createStation("1001000115", "방학", 1),
                createStation("1001000116", "창동", 1),
                createStation("1001000117", "녹천", 1),
                createStation("1001000118", "월계", 1),
                createStation("1001000119", "광운대", 1),
                createStation("1001000120", "석계", 1),
                createStation("1001000121", "신이문", 1),
                createStation("1001000122", "외대앞", 1),
                createStation("1001000123", "회기", 1),
                createStation("1001000124", "청량리", 1),
                createStation("1001000125", "제기동", 1),
                createStation("1001000126", "신설동", 1),
                createStation("1001000127", "동묘앞", 1),
                createStation("1001000129", "종로5가", 1),
                createStation("1001000130", "종로3가", 1),
                createStation("1001000131", "종각", 1),
                createStation("1001000132", "시청", 1),
                createStation("1001000133", "서울역", 1),
                createStation("1001000134", "남영", 1),
                createStation("1001000135", "용산", 1),
                createStation("1001000136", "노량진", 1),
                createStation("1001000137", "대방", 1),
                createStation("1001000138", "신길", 1),
                createStation("1001000139", "영등포", 1),
                createStation("1001000141", "구로", 1),
                createStation("1001000142", "구일", 1),
                createStation("1001000143", "개봉", 1),
                createStation("1001000144", "오류동", 1),
                createStation("1001000145", "온수", 1),
                createStation("1001080142", "가산디지털단지", 1),
                createStation("1001080143", "독산", 1),
                createStation("1001000128", "동대문", 1),
                createStation("1001000140", "신도림", 1),
                createStation("1002000201", "시청", 2),
                createStation("1002000202", "을지로입구", 2),
                createStation("1002000203", "을지로3가", 2),
                createStation("1002000204", "을지로4가", 2),
                createStation("1002000205", "동대문역사문화공원", 2),
                createStation("1002000206", "신당", 2),
                createStation("1002000207", "상왕십리", 2),
                createStation("1002000208", "왕십리", 2),
                createStation("1002000209", "한양대", 2),
                createStation("1002000210", "뚝섬", 2),
                createStation("1002000211", "성수", 2),
                createStation("1002000212", "건대입구", 2),
                createStation("1002000213", "구의", 2),
                createStation("1002000214", "강변", 2),
                createStation("1002000215", "잠실나루", 2),
                createStation("1002000216", "잠실", 2),
                createStation("1002000217", "잠실새내", 2),
                createStation("1002000218", "종합운동장", 2),
                createStation("1002000219", "삼성", 2),
                createStation("1002000220", "선릉", 2),
                createStation("1002000221", "역삼", 2),
                createStation("1002000222", "강남", 2),
                createStation("1002000223", "교대", 2),
                createStation("1002000224", "서초", 2),
                createStation("1002000225", "방배", 2),
                createStation("1002000226", "사당", 2),
                createStation("1002000227", "낙성대", 2),
                createStation("1002000228", "서울대입구", 2),
                createStation("1002000229", "봉천", 2),
                createStation("1002000230", "신림", 2),
                createStation("1002000231", "신대방", 2),
                createStation("1002000232", "구로디지털단지", 2),
                createStation("1002000233", "대림", 2),
                createStation("1002000234", "신도림", 2),
                createStation("1002000235", "문래", 2),
                createStation("1002000236", "영등포구청", 2),
                createStation("1002000237", "당산", 2),
                createStation("1002000238", "합정", 2),
                createStation("1002000239", "홍대입구", 2),
                createStation("1002000240", "신촌", 2),
                createStation("1002000241", "이대", 2),
                createStation("1002000242", "아현", 2),
                createStation("1002000243", "충정로", 2),
                createStation("1002002111", "용답", 2),
                createStation("1002002112", "신답", 2),
                createStation("1002002113", "용두", 2),
                createStation("1002002114", "신설동", 2),
                createStation("1002002341", "도림천", 2),
                createStation("1002002342", "양천구청", 2),
                createStation("1002002343", "신정네거리", 2),
                createStation("1002002344", "까치산", 2),
                createStation("1003000320", "구파발", 3),
                createStation("1003000321", "연신내", 3),
                createStation("1003000322", "불광", 3),
                createStation("1003000323", "녹번", 3),
                createStation("1003000324", "홍제", 3),
                createStation("1003000325", "무악재", 3),
                createStation("1003000326", "독립문", 3),
                createStation("1003000327", "경복궁", 3),
                createStation("1003000328", "안국", 3),
                createStation("1003000329", "종로3가", 3),
                createStation("1003000330", "을지로3가", 3),
                createStation("1003000331", "충무로", 3),
                createStation("1003000332", "동대입구", 3),
                createStation("1003000333", "약수", 3),
                createStation("1003000334", "금고", 3),
                createStation("1003000335", "옥수", 3),
                createStation("1003000336", "압구정", 3),
                createStation("1003000337", "신사", 3),
                createStation("1003000338", "잠원", 3),
                createStation("1003000339", "고속터미널", 3),
                createStation("1003000340", "교대", 3),
                createStation("1003000341", "남부터미널", 3),
                createStation("1003000342", "양재", 3),
                createStation("1003000343", "매봉", 3),
                createStation("1003000344", "도곡", 3),
                createStation("1003000345", "대치", 3),
                createStation("1003000346", "학여울", 3),
                createStation("1003000347", "대청", 3),
                createStation("1003000348", "일원", 3),
                createStation("1003000349", "수서", 3),
                createStation("1003000350", "가락시장", 3),
                createStation("1003000351", "경찰병원", 3),
                createStation("1003000352", "오금", 3),
                createStation("1004000409", "불암산", 4),
                createStation("1004000410", "상계", 4),
                createStation("1004000411", "노원", 4),
                createStation("1004000412", "창동", 4),
                createStation("1004000413", "쌍문", 4),
                createStation("1004000414", "수유", 4),
                createStation("1004000415", "미아", 4),
                createStation("1004000416", "미아사거리", 4),
                createStation("1004000417", "길음", 4),
                createStation("1004000418", "성신여대입구", 4),
                createStation("1004000419", "한성대입구", 4),
                createStation("1004000420", "혜화", 4),
                createStation("1004000421", "동대문", 4),
                createStation("1004000422", "동대문역사문화공원", 4),
                createStation("1004000423", "충무로", 4),
                createStation("1004000424", "명동", 4),
                createStation("1004000425", "회현", 4),
                createStation("1004000426", "서울역", 4),
                createStation("1004000427", "숙대입구", 4),
                createStation("1004000428", "삼각지", 4),
                createStation("1004000429", "신용산", 4),
                createStation("1004000430", "이촌", 4),
                createStation("1004000431", "동작", 4),
                createStation("1004000432", "이수", 4),
                createStation("1004000433", "사당", 4),
                createStation("1004000434", "남태령", 4)
        );

        try {
            stationRepository.saveAll(stations);
            log.info("=== 역 데이터 초기화 완료: {}개 역 ===", stations.size());
        } catch (Exception e) {
            log.error("역 데이터 초기화 실패: {}", e.getMessage(), e);
        }
    }

    private void initializeStoryData() {
        if (storyRepository.count() > 0) {
            return;
        }

        try {
            Station oksooStation = stationRepository.findByStaNameAndStaLine("옥수", 3)
                    .orElseThrow(() -> new RuntimeException("옥수역(3호선)을 찾을 수 없습니다."));

            Story story = Story.builder()
                    .station(oksooStation)
                    .stoTitle("옥수역의 잃어버린 시간")
                    .stoLength(8)
                    .build();
            Story savedStory = storyRepository.save(story);

            List<Page> pages = Arrays.asList(
                    createPage(savedStory.getStoId(), 1,
                            "늦은 밤 옥수역 승강장. 마지막 열차를 기다리는 당신 앞에 낡은 회중시계가 떨어져 있다.\n" +
                                    "시계는 멈춰있지만, 뒷면에는 \"시간을 되돌릴 수 있다면...\"이라는 글귀가 새겨져 있다.\n" +
                                    "갑자기 주변이 고요해지고, 역 안의 시계들이 모두 멈춰버린다."),

                    createPage(savedStory.getStoId(), 2,
                            "회중시계를 주워든 순간, 주변 풍경이 흐릿해진다.\n" +
                                    "옥수역이 1980년대의 모습으로 변하고, 낡은 제복을 입은 역무원이 당신을 쳐다본다.\n" +
                                    "\"또 시간여행자군요. 이번엔 무엇을 바꾸려고 하시나요?\""),

                    createPage(savedStory.getStoId(), 3,
                            "시계를 무시하고 지나치려 하지만, 발걸음이 무거워진다.\n" +
                                    "뒤돌아보니 시계에서 희미한 빛이 새어나오고 있다.\n" +
                                    "역 전체가 점점 어두워지면서 이상한 기운이 감돈다."),

                    createPage(savedStory.getStoId(), 4,
                            "역무원이 말한다. \"매일 밤 같은 일이 반복됩니다.\n" +
                                    "누군가 시계를 찾아가지만, 결국 후회만 안고 돌아오죠.\n" +
                                    "당신은 무엇을 후회하고 있나요?\""),

                    createPage(savedStory.getStoId(), 5,
                            "승강장 끝에서 시공간의 균열을 발견한다.\n" +
                                    "그 너머로 과거의 모습들이 스쳐 지나간다.\n" +
                                    "어릴 적 친구, 첫사랑, 돌아가신 할머니... 모든 기억이 되살아난다."),

                    createPage(savedStory.getStoId(), 6,
                            "시계를 돌리자 시간이 역행한다.\n" +
                                    "10년 전, 이 역에서 친구와 헤어졌던 그날로 돌아갔다.\n" +
                                    "이번엔 다른 선택을 할 수 있을까?"),

                    createPage(savedStory.getStoId(), 7,
                            "과거를 바꾸려 했지만, 결국 같은 결과가 반복된다.\n" +
                                    "역무원이 미소를 지으며 말한다. \"후회는 바꿀 수 없어요.\n" +
                                    "하지만 앞으로의 선택은 바꿀 수 있답니다.\""),

                    createPage(savedStory.getStoId(), 8,
                            "현재로 돌아온 당신. 회중시계는 사라지고 평범한 옥수역이다.\n" +
                                    "하지만 마음은 가벼워졌다. 과거에 얽매이지 않고,\n" +
                                    "지금 이 순간부터 새로운 선택을 하면 된다는 것을 깨달았다.")
            );
            pageRepository.saveAll(pages);

            createStoryOptions(pages);

        } catch (Exception e) {
            log.error("스토리 데이터 초기화 실패: {}", e.getMessage(), e);
        }
    }

    private void createStoryOptions(List<Page> pages) {
        Page page1 = pages.get(0);
        optionsRepository.saveAll(Arrays.asList(
                createOption(page1.getPageId(), "회중시계를 주워든다", "sanity", -5, pages.get(1).getPageId()),
                createOption(page1.getPageId(), "시계를 무시하고 지나간다", "health", -3, pages.get(2).getPageId()),
                createOption(page1.getPageId(), "주변을 둘러보며 상황을 파악한다", "sanity", 2, pages.get(1).getPageId())
        ));

        Page page2 = pages.get(1);
        optionsRepository.saveAll(Arrays.asList(
                createOption(page2.getPageId(), "역무원에게 무슨 일인지 묻는다", "sanity", 3, pages.get(3).getPageId()),
                createOption(page2.getPageId(), "시계를 되돌려 본다", "health", -5, pages.get(5).getPageId()),
                createOption(page2.getPageId(), "역 안을 탐색한다", "sanity", 0, pages.get(4).getPageId())
        ));

        Page page3 = pages.get(2);
        optionsRepository.saveAll(Arrays.asList(
                createOption(page3.getPageId(), "다시 시계로 돌아간다", "sanity", -3, pages.get(1).getPageId()),
                createOption(page3.getPageId(), "계속 무시하고 출구를 찾는다", "health", -8, pages.get(4).getPageId())
        ));

        Page page4 = pages.get(3);
        optionsRepository.saveAll(Arrays.asList(
                createOption(page4.getPageId(), "\"과거를 바꾸고 싶어요\"라고 답한다", "sanity", -5, pages.get(5).getPageId()),
                createOption(page4.getPageId(), "\"후회는 없습니다\"라고 답한다", "health", 5, pages.get(6).getPageId()),
                createOption(page4.getPageId(), "대답 대신 다른 질문을 한다", "sanity", 2, pages.get(4).getPageId())
        ));

        Page page5 = pages.get(4);
        optionsRepository.saveAll(Arrays.asList(
                createOption(page5.getPageId(), "균열로 들어간다", "health", -10, pages.get(5).getPageId()),
                createOption(page5.getPageId(), "균열을 관찰만 한다", "sanity", 3, pages.get(6).getPageId())
        ));

        Page page6 = pages.get(5);
        optionsRepository.saveAll(Arrays.asList(
                createOption(page6.getPageId(), "과거를 바꾸려 시도한다", "sanity", -10, pages.get(6).getPageId()),
                createOption(page6.getPageId(), "과거를 받아들인다", "health", 5, pages.get(6).getPageId())
        ));

        Page page7 = pages.get(6);
        optionsRepository.saveAll(Arrays.asList(
                createOption(page7.getPageId(), "역무원에게 감사를 표한다", "sanity", 10, pages.get(7).getPageId()),
                createOption(page7.getPageId(), "조용히 깨달음을 받아들인다", "health", 5, pages.get(7).getPageId())
        ));

        Page page8 = pages.get(7);
        optionsRepository.save(
                createOption(page8.getPageId(), "새로운 하루를 맞이한다", "health", 10, null)
        );

        log.info("선택지 생성 완료");
    }

    private Station createStation(String apiStationId, String staName, Integer staLine) {
        return Station.builder()
                .apiStationId(apiStationId)
                .staName(staName)
                .staLine(staLine)
                .build();
    }

    private Page createPage(Long storyId, long pageNumber, String content) {
        return Page.builder()
                .stoId(storyId)
                .pageNumber(pageNumber)
                .pageContents(content)
                .build();
    }

    private Options createOption(Long pageId, String content, String effect, int amount, Long nextPageId) {
        return Options.builder()
                .pageId(pageId)
                .optContents(content)
                .optEffect(effect)
                .optAmount(amount)
                .nextPageId(nextPageId)
                .build();
    }
}