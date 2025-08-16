package com.swj.shiwujie.blind;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.swj.shiwujie.R;
import com.swj.shiwujie.common.network.ApiCallback;
import com.swj.shiwujie.common.network.ApiService;
import com.swj.shiwujie.common.network.RetrofitClient;
import com.swj.shiwujie.common.utils.SharedPrefsUtil;
import com.swj.shiwujie.data.model.BlindVO;

public class EditProfileActivity extends AppCompatActivity {
    private ImageButton btnBack;
    private TextView tvUserId;
    private EditText etUsername;
    private TextView tvPhone;
    private EditText etEmail;
    private RadioGroup rgGender;
    private RadioButton rbMale;
    private RadioButton rbFemale;
    private EditText etIdCard;
    private EditText etOtherInfo;
    private TextView tvFamilyId;
    private Button btnChangePassword;
    private Button btnChangePhone;
    private Button btnConfirm;
    private ApiService apiService;
    private LinearLayout layoutDisabilityCard;
    private EditText etDisabilityCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        initServices();
        initViews();
        initListeners();
        loadUserInfo();
    }

    private void initServices() {
        apiService = RetrofitClient.getInstance().createService(ApiService.class);
        SharedPrefsUtil.init(this);
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvUserId = findViewById(R.id.tvUserId);
        etUsername = findViewById(R.id.etUsername);
        tvPhone = findViewById(R.id.tvPhone);
        etEmail = findViewById(R.id.etEmail);
        rgGender = findViewById(R.id.rgGender);
        rbMale = findViewById(R.id.rbMale);
        rbFemale = findViewById(R.id.rbFemale);
        etIdCard = findViewById(R.id.etIdCard);
        etOtherInfo = findViewById(R.id.etOtherInfo);
        tvFamilyId = findViewById(R.id.tvFamilyId);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnChangePhone = findViewById(R.id.btnChangePhone);
        btnConfirm = findViewById(R.id.btnConfirm);
        layoutDisabilityCard = findViewById(R.id.layoutDisabilityCard);
        etDisabilityCard = findViewById(R.id.etDisabilityCard);
    }

    private void initListeners() {
        btnBack.setOnClickListener(v -> {
            // 检查来源标记
            String source = getIntent().getStringExtra("source");
            
            if ("ai".equals(source)) {
                // 来自AI页面，需要特殊处理
                handleBackFromAI();
            } else {
                // 常规返回逻辑，保持不变
                finish();
            }
        });

        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        btnChangePhone.setOnClickListener(v -> showChangePhoneDialog());

        btnConfirm.setOnClickListener(v -> updateUserInfo());
    }

    private void loadUserInfo() {
        String token = SharedPrefsUtil.getToken();
        Long userId = SharedPrefsUtil.getUserId();

        if (token == null || userId == null) {
            Toast.makeText(this, "用户信息无效，请重新登录", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        apiService.getBlindById("Bearer " + token, userId).enqueue(new ApiCallback<BlindVO>(this) {
            @Override
            public void onSuccess(BlindVO data) {
                if (data != null) {
                    // 设置用户ID
                    tvUserId.setText("账号：" + data.getBlindId());
                    
                    // 设置用户名
                    etUsername.setText(data.getName());
                    
                    // 设置手机号
                    tvPhone.setText(data.getPhone());
                    
                    // 设置性别
                    if (data.getGender() != null) {
                        if (data.getGender() == 0) {
                            rbMale.setChecked(true);
                        } else if (data.getGender() == 1) {
                            rbFemale.setChecked(true);
                        }
                    }
                    
                    // 设置身份证号
                    if (data.getIdCard() != null) {
                        etIdCard.setText(data.getIdCard());
                    }
                    
                    // 设置其他信息
                    if (data.getOtherInfo() != null) {
                        etOtherInfo.setText(data.getOtherInfo());
                    }
                    
                    // 设置家庭ID
                    if (data.getFamilyId() != null) {
                        tvFamilyId.setText(String.valueOf(data.getFamilyId()));
                    } else {
                        tvFamilyId.setText("暂未加入家庭");
                    }

                    // 残疾证号动态显示与可编辑控制
                    layoutDisabilityCard.setVisibility(View.VISIBLE);
                    SharedPrefsUtil.setBoolean("isDisabilityCard", data.getIsDisabilityCard() != null && data.getIsDisabilityCard());
                    if (data.getIsDisabilityCard() == null || !data.getIsDisabilityCard()) {
                        etDisabilityCard.setEnabled(true);
                        etDisabilityCard.setFocusable(true);
                        etDisabilityCard.setFocusableInTouchMode(true);
                        etDisabilityCard.setText(data.getDisabilityCard() != null ? data.getDisabilityCard() : "");
                    } else {
                        etDisabilityCard.setText("已完成身份校验");
                        etDisabilityCard.setEnabled(false);
                        etDisabilityCard.setFocusable(false);
                    }
                }
            }
        });
    }

    private void updateUserInfo() {
        String token = SharedPrefsUtil.getToken();
        Long userId = SharedPrefsUtil.getUserId();

        if (token == null || userId == null) {
            Toast.makeText(this, "用户信息无效，请重新登录", Toast.LENGTH_SHORT).show();
            return;
        }

        String username = etUsername.getText().toString().trim();
        String idCard = etIdCard.getText().toString().trim();
        String otherInfo = etOtherInfo.getText().toString().trim();
        int gender = rbMale.isChecked() ? 0 : 1;

        if (TextUtils.isEmpty(username)) {
            Toast.makeText(this, "用户名不能为空", Toast.LENGTH_SHORT).show();
            return;
        }

        // 验证残疾证号长度
        // 删除：if (!TextUtils.isEmpty(disabilityCard) && disabilityCard.length() != 20) {
        // 删除：Toast.makeText(this, "残疾证号必须为20位", Toast.LENGTH_SHORT).show();
        // 删除：return;
        // }

        // 创建请求体
        BlindVO blind = new BlindVO();
        blind.setBlindId(userId);
        blind.setName(username);
        blind.setGender(gender);
        blind.setIdCard(idCard);
        blind.setOtherInfo(otherInfo);

        // 只有未完成身份校验时才校验和提交残疾证号
        String disabilityCard = null; // 默认为null
        if (etDisabilityCard.isEnabled()) {
            // 用户可以编辑残疾证，说明还未验证，需要传递新的残疾证号
            disabilityCard = etDisabilityCard.getText().toString().trim();
            if (TextUtils.isEmpty(disabilityCard)) {
                Toast.makeText(this, "残疾证号不能为空", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        // 如果已验证，disabilityCard保持为null，后端就不会进行验证
        blind.setDisabilityCard(disabilityCard);

        // 调用更新用户信息的API
        apiService.updateBlindInfo(
                "Bearer " + token,
                blind
        ).enqueue(new ApiCallback<Boolean>(this) {
            @Override
            public void onSuccess(Boolean response) {
                Toast.makeText(EditProfileActivity.this, "更新成功", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        builder.setView(dialogView);

        EditText etOriginPassword = dialogView.findViewById(R.id.etOriginPassword);
        EditText etNewPassword = dialogView.findViewById(R.id.etNewPassword);
        EditText etConfirmPassword = dialogView.findViewById(R.id.etConfirmPassword);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnConfirm = dialogView.findViewById(R.id.btnConfirm);

        AlertDialog dialog = builder.create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            String originPassword = etOriginPassword.getText().toString().trim();
            String newPassword = etNewPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword.getText().toString().trim();

            // 验证新密码
            if (TextUtils.isEmpty(newPassword)) {
                Toast.makeText(this, "新密码不能为空", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                Toast.makeText(this, "两次输入的密码不一致", Toast.LENGTH_SHORT).show();
                return;
            }

            // 调用修改密码API
            String token = SharedPrefsUtil.getToken();
            Long userId = SharedPrefsUtil.getUserId();

            if (token == null || userId == null) {
                Toast.makeText(this, "用户信息无效，请重新登录", Toast.LENGTH_SHORT).show();
                return;
            }

            apiService.updateBlindPassword(
                    "Bearer " + token,
                    userId,
                    originPassword,
                    newPassword
            ).enqueue(new ApiCallback<Boolean>(this) {
                @Override
                public void onSuccess(Boolean response) {
                    Toast.makeText(EditProfileActivity.this, "密码修改成功", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(EditProfileActivity.this, "修改失败：" + message, Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    private void showChangePhoneDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_phone, null);
        builder.setView(dialogView);

        EditText etNewPhone = dialogView.findViewById(R.id.etNewPhone);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnConfirm = dialogView.findViewById(R.id.btnConfirm);

        AlertDialog dialog = builder.create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            String newPhone = etNewPhone.getText().toString().trim();

            // 验证手机号
            if (TextUtils.isEmpty(newPhone)) {
                Toast.makeText(this, "手机号不能为空", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newPhone.length() != 11) {
                Toast.makeText(this, "请输入11位手机号", Toast.LENGTH_SHORT).show();
                return;
            }

            // 调用换绑手机号API
            String token = SharedPrefsUtil.getToken();
            Long userId = SharedPrefsUtil.getUserId();

            if (token == null || userId == null) {
                Toast.makeText(this, "用户信息无效，请重新登录", Toast.LENGTH_SHORT).show();
                return;
            }

            apiService.updateBlindPhone(
                    "Bearer " + token,
                    userId,
                    newPhone
            ).enqueue(new ApiCallback<Boolean>(this) {
                @Override
                public void onSuccess(Boolean response) {
                    Toast.makeText(EditProfileActivity.this, "手机号修改成功", Toast.LENGTH_SHORT).show();
                    // 更新界面显示的手机号
                    tvPhone.setText(newPhone);
                    dialog.dismiss();
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(EditProfileActivity.this, "修改失败：" + message, Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }
    
    /**
     * 处理来自AI页面的返回逻辑
     */
    private void handleBackFromAI() {
        // 来自AI页面，直接返回到AI页面
        // 通过Activity的finish()返回到上一个页面，即AI页面
        finish();
    }
} 