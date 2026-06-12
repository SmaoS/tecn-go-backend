package com.tecngo.app_versions.service;
import com.tecngo.app_versions.dto.*;
import com.tecngo.app_versions.entity.*;
import com.tecngo.app_versions.repository.AppVersionRepository;
import com.tecngo.shared.exception.NotFoundException;
import com.tecngo.system_parameters.service.SystemParameterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.regex.Pattern;

@Service @RequiredArgsConstructor
public class AppVersionService {
    private static final Pattern SEMVER = Pattern.compile("^\\d+\\.\\d+\\.\\d+$");
    private final AppVersionRepository repository;
    private final SystemParameterService parameters;
    @Transactional(readOnly=true)
    public AppVersionCheckResponse check(AppPlatform platform, String currentVersion) {
        validateVersion(currentVersion);
        AppVersion item = repository.findByPlatformAndActiveTrue(platform)
                .orElseThrow(() -> new NotFoundException("No active app version configuration for " + platform));
        boolean belowMinimum = compare(currentVersion, item.getMinimumSupportedVersion()) < 0;
        boolean belowLatest = compare(currentVersion, item.getLatestVersion()) < 0;
        boolean enabled = parameters.appVersionCheckEnabled();
        boolean forced = enabled && (belowMinimum || item.isForceUpdate());
        return new AppVersionCheckResponse(platform, currentVersion, item.getLatestVersion(),
                item.getMinimumSupportedVersion(), enabled && (belowLatest || item.isForceUpdate()), forced,
                item.getUpdateUrl(), item.getMessage());
    }
    @Transactional(readOnly=true) public List<AppVersionResponse> list(){return repository.findAll().stream().map(this::map).toList();}
    @Transactional
    public AppVersionResponse update(AppPlatform platform, UpdateAppVersionRequest input) {
        validateVersion(input.minimumSupportedVersion()); validateVersion(input.latestVersion());
        if (compare(input.minimumSupportedVersion(), input.latestVersion()) > 0)
            throw new IllegalArgumentException("minimumSupportedVersion cannot exceed latestVersion");
        if (input.forceUpdate() && (input.updateUrl()==null || input.updateUrl().isBlank()))
            throw new IllegalArgumentException("updateUrl is required when forceUpdate is true");
        AppVersion item=repository.findByPlatform(platform).orElseThrow(()->new NotFoundException("App version not found"));
        item.setMinimumSupportedVersion(input.minimumSupportedVersion());
        item.setLatestVersion(input.latestVersion()); item.setForceUpdate(input.forceUpdate());
        item.setUpdateUrl(input.updateUrl()==null?"":input.updateUrl().trim());
        item.setMessage(input.message().trim()); item.setActive(input.active());
        return map(repository.save(item));
    }
    public int compare(String left,String right){validateVersion(left);validateVersion(right);int[] a=parse(left),b=parse(right);for(int i=0;i<3;i++){int c=Integer.compare(a[i],b[i]);if(c!=0)return c;}return 0;}
    private int[] parse(String value){return Arrays.stream(value.split("\\.")).mapToInt(Integer::parseInt).toArray();}
    private void validateVersion(String value){if(value==null||!SEMVER.matcher(value).matches())throw new IllegalArgumentException("Version must use semantic format x.y.z");}
    private AppVersionResponse map(AppVersion i){return new AppVersionResponse(i.getId(),i.getPlatform(),i.getMinimumSupportedVersion(),i.getLatestVersion(),i.isForceUpdate(),i.getUpdateUrl(),i.getMessage(),i.isActive(),i.getCreatedAt(),i.getUpdatedAt());}
}
