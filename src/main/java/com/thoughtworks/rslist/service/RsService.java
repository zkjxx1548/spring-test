package com.thoughtworks.rslist.service;

import com.thoughtworks.rslist.domain.Trade;
import com.thoughtworks.rslist.domain.Vote;
import com.thoughtworks.rslist.dto.RsEventDto;
import com.thoughtworks.rslist.dto.TradeDto;
import com.thoughtworks.rslist.dto.UserDto;
import com.thoughtworks.rslist.dto.VoteDto;
import com.thoughtworks.rslist.exception.BuyRsEventRankFailException;
import com.thoughtworks.rslist.repository.RsEventRepository;
import com.thoughtworks.rslist.repository.TradeRepository;
import com.thoughtworks.rslist.repository.UserRepository;
import com.thoughtworks.rslist.repository.VoteRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RsService {
  final RsEventRepository rsEventRepository;
  final UserRepository userRepository;
  final VoteRepository voteRepository;
  final TradeRepository tradeRepository;

  public RsService(RsEventRepository rsEventRepository, UserRepository userRepository, VoteRepository voteRepository, TradeRepository tradeRepository) {
    this.rsEventRepository = rsEventRepository;
    this.userRepository = userRepository;
    this.voteRepository = voteRepository;
    this.tradeRepository = tradeRepository;
  }

  public void vote(Vote vote, int rsEventId) {
    Optional<RsEventDto> rsEventDto = rsEventRepository.findById(rsEventId);
    Optional<UserDto> userDto = userRepository.findById(vote.getUserId());
    if (!rsEventDto.isPresent()
        || !userDto.isPresent()
        || vote.getVoteNum() > userDto.get().getVoteNum()) {
      throw new RuntimeException();
    }
    VoteDto voteDto =
        VoteDto.builder()
            .localDateTime(vote.getTime())
            .num(vote.getVoteNum())
            .rsEvent(rsEventDto.get())
            .user(userDto.get())
            .build();
    voteRepository.save(voteDto);
    UserDto user = userDto.get();
    user.setVoteNum(user.getVoteNum() - vote.getVoteNum());
    userRepository.save(user);
    RsEventDto rsEvent = rsEventDto.get();
    rsEvent.setVoteNum(rsEvent.getVoteNum() + vote.getVoteNum());
    rsEventRepository.save(rsEvent);
  }

  //id = rsEvent id;
  public void buy(Trade trade, int id) {
    RsEventDto rsEventDto = rsEventRepository.findById(id).get();
    RsEventDto originRsEventDto = rsEventRepository.findByRank(trade.getRank()).get();

    if (originRsEventDto.getAmount() < trade.getAmount()) {
      rsEventRepository.deleteByRank(trade.getRank());
      rsEventRepository.deleteById(id);
      rsEventRepository.save(
              RsEventDto.builder()
                      .user(rsEventDto.getUser())
                      .eventName(rsEventDto.getEventName())
                      .keyword(rsEventDto.getKeyword())
                      .voteNum(originRsEventDto.getVoteNum())
                      .id(rsEventDto.getId())
                      .rank(trade.getRank())
                      .build());
      tradeRepository.save(
              TradeDto.builder()
                      .amount(trade.getAmount())
                      .rank(trade.getRank())
                      .rsEvent(rsEventDto)
                      .build());
    } else {
      throw new BuyRsEventRankFailException();
    }
  }

  public List<RsEventDto> sortRsEventByVoteNum(List<RsEventDto> rsEventDtoList) {
    return rsEventDtoList.stream()
            .sorted(Comparator.comparingInt(RsEventDto::getVoteNum).reversed())
            .collect(Collectors.toList());
  }

  public List<RsEventDto> setRankOfRsEventAfterSort(List<RsEventDto> rsEventDtoList) {
    for (int i = 0; i < rsEventDtoList.size(); i++) {
      rsEventDtoList.get(i).setRank(i + 1);
    }
    return rsEventDtoList;
  }

  public List<RsEventDto> getHadRankedRsEventAfterSave(List<RsEventDto> rsEventDtoList) {
    return setRankOfRsEventAfterSort(sortRsEventByVoteNum(rsEventDtoList));
  }
}