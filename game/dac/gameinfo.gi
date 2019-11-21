"GameInfo"
{
	//
	// gameinfo.gi is the non-branch-varying content and can be integrated between branches.
	// Branch-varying info, such as the game/title and app IDs, is in gameinfo_branchspecific.gi.
	//

	type		multiplayer_only
	
	SupportsDX8 0
	nodegraph 0
	nomodels 1
	nohimodel 1
	nocrosshair 0
	SupportsDX8	0
	nodegraph 0
	tonemapping 0 // Hide tonemapping ui in tools mode
	GameData	"dac.fgd"

	FileSystem
	{
		//
		// The code that loads this file automatically does a few things here:
		//
		// 1. For each "Game" search path, it adds a "GameBin" path, in <dir>\bin
		// 2. For each "Game" search path, it adds another "Game" path in front of it with _<langage> at the end.
		//    For example: c:\hl2\cstrike on a french machine would get a c:\hl2\cstrike_french path added to it.
		// 3. For the first "Game" search path, it adds a search path called "MOD".
		// 4. For the first "Game" search path, it adds a search path called "DEFAULT_WRITE_PATH".
		//

		//
		// Search paths are relative to the exe directory\..\
		//
		SearchPaths
		{
			// These are optional low-violence paths. They will only get mounted if you're in a low-violence mode.
			Game_LowViolence	dac_lv

			Game_NonTools		dac_desktop [!$MOBILE && !$MOBILE_RSRC && !$BUILDALLSHADERS_RUN && !$CONTENTBUILDER_RUN && !$VMPI_GAMEINFO]
			Game_NonTools		dac_mobile [$MOBILE || $MOBILE_RSRC]
			Game				dac
			Game				core
			Mod					dac
			Write				dac

			LayeredGameRoot		"../game_otherplatforms/etc" [$MOBILE || $ETC_TEXTURES] //Some platforms do not support DXT compression. ETC is a well-supported alternative.
		}
	}

	MaterialSystem2
	{
		RenderModes
		{
			"game" "Default"
			"game" "DotaDeferred" [!$MOBILE] //We don't run the deferred rendering pipeline on mobile
			"game" "DotaForward"
			"game" "Depth"

			"tools" "ToolsVis" // Visualization modes for all shaders (lighting only, normal maps only, etc.)
			"tools" "ToolsWireframe" // This should use the ToolsVis mode above instead of being its own mode
			"tools" "ToolsUtil" // Meant to be used to render tools sceneobjects that are mod-independent, like the origin grid
		}
	}

	NetworkSystem
	{
		"CreateListenSocketP2P" "0"
	}

	Engine2
	{
		"HasModAppSystems" "1"
		"Capable64Bit" "1"
		"UsesScaleform" "0"
		"UsesVGui" "0"
		"UsesPanorama" "1"
		"HasGameUI" "0"
		"GameUIFromClient" "0"
		"PanoramaUIClientFromClient" "1"
		"SetUILanguageOnSteamDropDown" "1"
		"ClientSkipsServerDLL" "0"
		"MinimumPhysicalMemoryMB" "1536" [$MOBILE]

		"RenderingPipeline"
		{
			"SkipPostProcessing" "1"
			"SupportsMSAA" "0"
		}

		"BugBait"
		{
			// Used by 'bug:' in chat to normalize report settings during playtests
			"Owner" "triage*"
			"Severity" "high"
			"Priority" "none"
			"Category" "---"
			"Product" "dac"
			"Component" "dac"
		}
	}


	ToolsEnvironment
	{
		"Engine"	"Source 2"
		"ToolsDir"	"../sdktools"	// NOTE: Default Tools path. This is relative to the mod path.
	}

	Hammer
	{
		"fgd"					"dac.fgd"	// NOTE: This is relative to the 'mod' path.
		"DefaultTextureScale"	"0.250000"
		"DefaultSolidEntity"	"trigger_multiple"
		"DefaultPointEntity"	"info_player_start"
		"NavMarkupEntity"		"func_nav_markup"
		"LoadScriptEntities"	"0"
		"GameFeatureSet"		"Dota"
		"DefaultGridTileSet"	"/maps/tilesets/radiant_basic.vmap"
		"DefaultGridTileSet2"	"/maps/tilesets/dire_basic.vmap"
	}
	
	ItemEditor
	{
		"edit_item_rank"				"1"
	}

	ResourceCompiler
	{
		// Overrides of the default builders as specified in code, this controls which map builder steps
		// will be run when resource compiler is run for a map without specifiying any specific map builder
		// steps. Additionally this controls which builders are displayed in the hammer build dialog.
		DefaultMapBuilders
		{
			"light"		"0"	// Dota does not use baked lighting
			"envmap"	"0"	// Dota doesn't generate environment maps from the map
			"gridnav"	"1"	// Dota generates its grid navigation data by default
		}
		
		MeshCompiler
		{
			PerDrawCullingData      "0"
			EncodeVertexBuffer      "1"	// Compress VBs with meshoptimizer
			EncodeIndexBuffer       "1"	// Compress IBs with meshoptimizer
        }

		TextureCompiler
		{
			CompressMipsOnDisk      "1"		// Enable LZ4 compression of textures to reduce on-disk/download size
			CompressMinRatio        "95"	// Compress only if compressed version is < 95% size of original
		}
	}

	MaterialEditor
	{
		"DefaultShader"			"global_lit_simple"
		"ExpressionHelpUrl"		"https://intranet.valvesoftware.com/index.php/Source_2.0/Shader_Format#Shader.2FMaterial_Expression_Syntax"
	}

	RenderPipelineAliases
	{
		"Tools"			"Dota:Forward"
		"EnvMapBake"	"Dota"
	}

	RenderSystem
	{
		"VulkanUseTransformConstantBuffer"	"1" [$ANDROID] // Snapdragon driver punts to immediate mode when we use vertex texture fetch, so use UBO to store transform data.
		"VulkanUseStreamingTextureManager"	"1"
		"VulkanUseSecondaryCommandBuffers"	"1" // Use secondary command buffers for more efficiency on tiled based renderers. Desktop too to limit configurations.
		"VulkanOnly"						"1"	[ $LINUX || $OSX ] // No OpenGL or D3D9/11 fallback on Linux or OSX, only Vulkan is supported.
		"VulkanUseExternalSubpassDependency"	"1"	// Required synchronization for ARM Mali GPUs
		"SheetCacheTextureRows"				"1024"
		"SheetCacheTextureCols"				"128"
	}

	SceneSystem
	{
		"NoSunLightManager" "1"
		"TransformTextureRowCount" "64"		[$MOBILE]
		"CMTAtlasWidthMobile" "512"
		"CMTAtlasHeightMobile" "512"	
		"CMTAtlasChunkSizeMobile" "128"
		"CMTAtlasWidth" "512"
		"CMTAtlasHeight" "512"
		"CMTAtlasChunkSize" "128"
		"DrawParticleChildrenSeparateFromParents" "1"
		"SkipLoadingRenderingPipelines" "1"
		"MaxAutoPartitions" "8"				[!$MOBILE]
		"MaxAutoPartitions" "6"				[$MOBILE]
	}

	SoundSystem
	{
		"DisableSteamAudio" "1"
		"DisableSoundOnDedicatedServer" "1"
		"WaveDataCacheSizeMB" "32" [$MOBILE]
	}

	AnimationSystem
	{
		"DisableAnimationScript" "1"
	}

	Localize
	{
		"DuplicateTokensAssert" "1"
	}
}
