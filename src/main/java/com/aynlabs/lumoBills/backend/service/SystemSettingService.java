package com.aynlabs.lumoBills.backend.service;

import com.aynlabs.lumoBills.backend.entity.SystemSetting;
import com.aynlabs.lumoBills.backend.repository.SystemSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SystemSettingService {

    private final SystemSettingRepository repository;

    public String getValue(String key, String defaultValue) {
        return repository.findBySettingKey(key)
                .map(SystemSetting::getSettingValue)
                .orElse(defaultValue);
    }

    @Transactional
    public void setValue(String key, String value) {
        SystemSetting setting = repository.findBySettingKey(key)
                .orElse(new SystemSetting(key, value));
        setting.setSettingKey(key);
        setting.setSettingValue(value);
        repository.save(setting);
    }
}
