<template>
	<view class="upload-license">
		<view class="certificate" @click="uploadChange('front')">
			<view class="mb-1">{{ frontDesc }}</view>
			<view v-if="frontImg" class="card-img p-2">
				<image :src="front" class="w-full h-[170px]" />
			</view>
			<image v-else :src="defaultFrontSrc" class="w-full h-[170px] box-img" />
		</view>
		<view class="certificate" @click="uploadChange('back')">
			<view class="mb-1">{{ backDesc }}</view>
			<view v-if="backImg" class="card-img p-2">
				<image :src="back" class="w-full h-[172px]" />
			</view>
			<image v-else :src="defaultBackSrc" class="w-full h-[172px] box-img" />
		</view>
	</view>
</template>

<script lang="ts" setup>
	import defaultFront from "./images/idcard-front.png";
	import defaultBack from "./images/idcard-back.png";
	import { computed } from "vue";
	// import { fileAPI } from "@/service/my";
	/// ///////////////////////////////////////////////////////////////////////////////
	// #region ----变量等------------------------------------------------------------------
	const props = defineProps({
		frontImg: {
			// 正面图
			type: String,
		},
		backImg: {
			// 反面图
			type: String,
		},
		frontDesc: {
			// 正面描述
			type: String,
			default: "身份证人像面",
		},
		backDesc: {
			// 反面描述
			type: String,
			default: "身份证国徽面",
		},
		defaultFrontSrc: {
			// 正面默认图片
			type: String,
			default: defaultFront,
		},
		defaultBackSrc: {
			// 反面默认图片
			type: String,
			default: defaultBack,
		},
	});

	const emits = defineEmits(["update:frontImg", "update:backImg"]);

	const front = computed(() => {
		return props.frontImg;
	});

	const back = computed(() => {
		return props.backImg;
	});
	// #endregion ========================================================================
	/// ///////////////////////////////////////////////////////////////////////////////
	// #region ----初始化,生命周期等-------------------------------------------------------

	// #endregion ========================================================================
	/// //////////////////////////////////////////////////////////////////////////////////
	// #region ----操作方法----------------------------------------------------------------
	/** 上传图片1 */
	function uploadChange(flag : string) {
		uni.chooseImage({
			count: 1, // 默认9
			success: res => {
				// 不走接口的
				if (flag === "front") {
					emits("update:frontImg", res.tempFiles[0].path);
				} else {
					emits("update:backImg", res.tempFiles[0].path);
				}
				// 走上传接口
				// const uploadParams = {
				//   type: 10,
				//   cid: 0,
				// };
				// fileAPI(res.tempFiles[0].path, uploadParams).then((res: any) => {
				//   if (res.code === 1) {
				//     if (flag === "front") {
				//       emits("update:frontImg", res.data.url);
				//     } else {
				//       emits("update:backImg", res.data.url);
				//     }
				//   }
				// });
			},
		});
	}

	// #endregion ========================================================================
	/// //////////////////////////////////////////////////////////////////////////////////
	// #region ----其他方法----------------------------------------------------------------

	// #endregion ========================================================================
	/// /////////////////////////////////////////////////////////////////////////////////
</script>

<style lang="scss" scoped>
	.mb-1 {
		margin-bottom: 0.25rem;
	}
	.p-2 {
		padding: 0.5rem;
	}
	.w-full {
		width: 100%;
	}
	.upload-license {
		display: flex;
		flex-direction: column;
		gap: 10rpx;
		width: 100%;
		height: 100%;
		color: #484848;

		.box-img {
			border: 5rpx dashed #ddd;
			border-radius: 25rpx;
		}

		.card-img {
			box-sizing: border-box;
			border: 5rpx dashed #ddd;
			border-radius: 25rpx;

			image {
				border-radius: 25rpx;
			}
		}
	}
</style>